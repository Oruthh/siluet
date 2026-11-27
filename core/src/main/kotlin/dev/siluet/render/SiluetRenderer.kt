package dev.siluet.render

import dev.siluet.exception.internal.ComponentViewNotFoundException
import dev.siluet.exception.internal.SectionsGroupNotFoundException
import dev.siluet.blueprint.Template
import dev.siluet.directive.Directive
import dev.siluet.exception.SiluetBlueprintException
import dev.siluet.exception.internal.LoopLimitExceededException
import dev.siluet.exception.internal.RenderException
import dev.siluet.exception.internal.SectionViewNotFoundException
import dev.siluet.exception.internal.SectionsGroupLoadingException
import dev.siluet.filter.Filter
import dev.siluet.html.HTMLEscape
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.locale.LocaleRegistry
import dev.siluet.pagination.PaginationContext
import dev.siluet.pagination.PaginationSource
import dev.siluet.parser.ComponentNode
import dev.siluet.reflect.CachedReflectionPropertyAccessor
import dev.siluet.parser.DirectiveNode
import dev.siluet.parser.ForNode
import dev.siluet.parser.IfNode
import dev.siluet.parser.PaginateNode
import dev.siluet.parser.PrintNode
import dev.siluet.parser.SectionsNode
import dev.siluet.parser.SiluetNode
import dev.siluet.parser.TextNode
import dev.siluet.reflect.PropertyAccessor
import dev.siluet.render.stack.RenderStack
import dev.siluet.theme.SiluetTheme

internal class SiluetRenderer(
    private val maxLoopIterations: Int,
    private val maxRenderDepth: Int,
    private val strictMode: Boolean,
    private val accessor: PropertyAccessor = CachedReflectionPropertyAccessor(),
    private val filters: Map<String, Filter> = emptyMap(),
    private val directives: Map<String, Directive> = emptyMap()
) {
    fun render(nodes: List<SiluetNode>, model: Any?, template: Template, theme: SiluetTheme, contextBuilder: RenderContext.Builder.() -> Unit = {}): String {
        val renderContext = RenderContext.Builder().apply (contextBuilder).build(theme.translations)

        val buffer = StringBuilder()
        val evaluationContext = EvaluationContext(
            root = model,
            accessor = accessor,
            filters = filters,
            directives = directives,
            template = template,
            localeRegistry = theme.translations,
            locale = renderContext.locale
        )
        val evaluator = ExpressionEvaluator(
            context = evaluationContext,
            strictMode = strictMode
        )

        val renderStack = RenderStack(maxRenderDepth)

        renderNodes(
                nodes = nodes,
                session = RenderSession(
                    theme = theme,
                    renderContext = renderContext,
                    renderStack = renderStack,
                    evaluationContext = evaluationContext,
                    evaluator = evaluator,
                    buffer = buffer
                )
            )
        return buffer.toString()
    }

    private fun renderNodes(
        nodes: List<SiluetNode>,
        session: RenderSession
    ) {
        for(node in nodes) {
            renderNode(
                node = node,
                session = session
            )
        }
    }

    private fun renderNode(
        node: SiluetNode,
        session: RenderSession
    ) {
        when (node) {
            is TextNode -> session.buffer.append(node.text)

            is PrintNode -> {
                val rawValue = session.evaluator.evaluate(node.expression)
                val filteredValue = session.evaluator.applyFilters(rawValue, node.filters, session.renderContext)
                val escapedValue = HTMLEscape.escape(filteredValue)

                if(filteredValue != null) {
                    session.buffer.append(escapedValue)
                }
            }

            is IfNode -> {
                val conditionResult = session.evaluator.isTruthy(session.evaluator.evaluate((node.condition)))
                if(conditionResult) {
                    renderNodes(
                        nodes = node.thenBody,
                        session = session
                    )
                } else if(node.elseBody != null) {
                    renderNodes(
                        nodes = node.elseBody,
                        session = session
                    )
                }
            }

            is ForNode -> {
                val collection = session.evaluator.evaluate(node.collectionExpression)
                val items = try{
                     toIterable(collection)
                } catch (e: IllegalArgumentException) {
                    throw RenderException(
                        e.message!!,
                        sourceLocation = node.sourceLocation
                    )
                }

                if(items is Collection<*> && items.size > maxLoopIterations) {
                    throw LoopLimitExceededException(
                        maxLoopIterations,
                        sourceLocation = node.sourceLocation
                    )
                }

                session.evaluationContext.pushScope()
                try {
                    var iterations = 0

                    for (item in items) {
                        if(++iterations > maxLoopIterations) {
                            throw LoopLimitExceededException(
                                maxLoopIterations,
                                sourceLocation = node.sourceLocation
                            )
                        }

                        session.evaluationContext.setLocal(node.variableName, item)
                        renderNodes(
                            nodes = node.body,
                            session = session
                        )
                    }
                } finally {
                    session.evaluationContext.popScope()
                }
            }

            is ComponentNode -> {
                val componentName = session.evaluator.evaluate(node.componentName)?.toString()
                    ?: throw RenderException(
                        "Component name evaluated to null",
                        sourceLocation = node.sourceLocation
                    )

                val componentView = try {
                    session.theme.getComponentView(componentName)
                } catch (e: ComponentViewNotFoundException) {
                    throw RenderException(
                        e.message!!,
                        sourceLocation = node.sourceLocation
                    )
                }

                val properties = node.properties.mapValues {
                    session.evaluator.evaluate(it.value)
                }

                val componentEvaluationContext = EvaluationContext(
                    root = properties,
                    accessor = accessor,
                    filters = filters,
                    directives = directives,
                    template = session.evaluationContext.template,
                    localeRegistry = session.theme.translations,
                    locale = session.renderContext.locale
                )

                val componentEvaluator = ExpressionEvaluator(
                    context = componentEvaluationContext,
                    strictMode = strictMode
                )

                session.renderStack.withFrame(
                    sourceName = componentView.sourceName,
                    callSite = node.sourceLocation
                ){
                    renderNodes(
                        nodes = componentView.ast,
                        session = session.copy(
                            evaluationContext = componentEvaluationContext,
                            evaluator = componentEvaluator
                        )
                    )
                }
            }

            is SectionsNode -> {
                val sectionsGroupName = if(node.sectionsGroupName != null) {
                    session.evaluator.evaluate(node.sectionsGroupName)?.toString()
                        ?: throw RenderException(
                            "Sections group name evaluated to null",
                            sourceLocation = node.sourceLocation
                        )
                } else {
                    null
                }

                val sections = if(sectionsGroupName != null) {
                    try{
                        session.theme.getSectionsGroup(sectionsGroupName).sections
                    } catch (e: SectionsGroupNotFoundException) {
                        throw RenderException(
                            e.message!!,
                            sourceLocation = node.sourceLocation
                        )
                    } catch (e: SectionsGroupLoadingException) {
                        throw SiluetBlueprintException(
                            e.message!!,
                            sourceName = e.sourceName
                        )
                    }
                } else {
                    session.evaluationContext.template.sections
                }

                for(section in sections) {
                    val sectionView = try {
                        session.theme.getSectionView(section.name)
                    } catch (e: SectionViewNotFoundException) {
                        throw RenderException(
                            e.message!!,
                            sourceLocation = node.sourceLocation
                        )
                    }

                    session.renderStack.withFrame(
                        sourceName = sectionView.sourceName,
                        callSite = node.sourceLocation
                    ) {
                        session.evaluationContext.pushScope()
                        session.evaluationContext.setLocal("section", section)
                        try{
                            this.renderNodes(
                                nodes = sectionView.ast,
                                session = session
                            )
                        } finally {
                            session.evaluationContext.popScope()
                        }
                    }
                }
            }

            is PaginateNode -> {
                val rawCollection = session.evaluator.evaluate(node.collectionExpression)
                val variableName = node.variableName

                val rawPageSize = session.evaluator.evaluate(node.pageSizeExpression)

                val pageSize = (rawPageSize as? Number)?.toInt()?.coerceAtLeast(1) ?: 10

                val currentPage = if (node.pageNumberExpression != null){
                    (session.evaluator.evaluate(node.pageNumberExpression) as? Number)?.toInt()
                        ?: throw RenderException(
                            "Page must be a integer",
                            sourceLocation = node.pageNumberExpression.sourceLocation
                        )
                } else {
                    //TODO are we sure that we want to left this?
                    (session.evaluationContext.variableOrNull("currentPage") as? Number)?.toInt()
                        ?: (session.evaluationContext.variableOrNull("pageNumber") as? Number)?.toInt()
                        ?: 1
                }

                val paginationContext = when(rawCollection) {
                    is PaginationSource<*> -> {
                        rawCollection.fetchPage(currentPage, pageSize)
                    }
                    is List<*> -> {
                        val offset = (currentPage - 1) * pageSize
                        val pagedSlice = rawCollection.drop(offset).take(pageSize)
                        PaginationContext.of(pagedSlice, currentPage, pageSize, rawCollection.size)
                    }
                    is Sequence<*> -> {
                        val offset = (currentPage - 1) * pageSize
                        val pagedSlice = rawCollection.drop(offset).take(pageSize).toList()
                        PaginationContext.of(pagedSlice, currentPage, pageSize)
                    }
                    else -> {
                        //TODO maybe we should throw a exception instead of passing empty list?
                        PaginationContext.of(emptyList(), currentPage, pageSize, 0)
                    }
                }

                session.evaluationContext.pushScope()
                try {
                    session.evaluationContext.setLocal(variableName, paginationContext)
                    renderNodes(
                        nodes = node.body,
                        session = session
                    )
                } finally {
                    session.evaluationContext.popScope()
                }
            }

            is DirectiveNode -> {
                val directive = session.evaluationContext.directives[node.name]
                    ?: throw RenderException(
                        "Unknown directive: @${node.name}",
                        sourceLocation = node.sourceLocation
                    )

                val args = node.arguments.map { session.evaluator.evaluate(it) }
                val namedArgs = node.namedArguments.mapValues { session.evaluator.evaluate(it.value) }

                var blockFailure: Throwable? = null

                val bodySupplier: (() -> String)? = node.body?.let { bodyNodes -> {
                    try{
                        val bodyBuffer = StringBuffer()
                        renderNodes(
                            nodes = bodyNodes,
                            session = session.copy(
                                buffer = bodyBuffer
                            )
                        )
                        bodyBuffer.toString()
                    } catch (e: Throwable) {
                        blockFailure = e
                        throw e
                    }
                }}

                try{
                    directive.execute(args, namedArgs, bodySupplier, session.renderContext, session.buffer)
                } catch (e: Exception) {
                    if(e === blockFailure) {
                        throw e
                    }

                    throw RenderException(
                        "Directive '${node.name}' failed: ${e.message ?: e::class.simpleName}",
                        sourceLocation = node.sourceLocation,
                    )
                }
            }
        }
    }

    private fun toIterable(value: Any?): Iterable<*> {
        return when (value) {
            null -> emptyList<Any?>()
            is Iterable<*> -> value
            is Array<*> -> value.asIterable()
            is Map<*, *> -> value.entries
            is Sequence<*> -> value.asIterable()
            else -> throw IllegalArgumentException("Value of type '${value::class.simpleName}' cannot be iterated in @for")
        }
    }
}