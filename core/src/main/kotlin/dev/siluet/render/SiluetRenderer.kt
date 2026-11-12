package dev.siluet.render

import dev.siluet.exception.ComponentViewNotFoundException
import dev.siluet.exception.SectionsGroupNotFoundException
import dev.siluet.blueprint.Template
import dev.siluet.exception.LoopLimitExceededException
import dev.siluet.exception.RenderException
import dev.siluet.exception.SectionViewNotFoundException
import dev.siluet.filter.Filter
import dev.siluet.html.HTMLEscape
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.locale.LocaleNotFoundException
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
import dev.siluet.parser.TranslationNode
import dev.siluet.reflect.PropertyAccessor

internal class SiluetRenderer(
    private val maxLoopIterations: Int,
    private val accessor: PropertyAccessor = CachedReflectionPropertyAccessor(),
    private val filters: Map<String, Filter> = emptyMap(),
    private val directives: Map<String, Directive> = emptyMap(),
    private val viewsLoader: SiluetViewsLoader,
    private val sectionsGroupsLoader: SiluetSectionsGroupLoader,
    private val localeRegistry: LocaleRegistry
) {
    fun render(nodes: List<SiluetNode>, model: Any?, template: Template, contextBuilder: RenderContext.Builder.() -> Unit = {}): String {
        val renderContextBuilder = RenderContext.Builder()
        renderContextBuilder.contextBuilder()
        val renderContext = renderContextBuilder.build()

        val buffer = StringBuilder()
        val evaluationContext = EvaluationContext(
            root = model,
            accessor = accessor,
            filters = filters,
            directives = directives,
            template = template,
            localeRegistry = localeRegistry,
            locale = renderContext.locale
        )
        val evaluator = ExpressionEvaluator(evaluationContext)

        renderNodes(nodes, renderContext, evaluationContext, evaluator, buffer)
        return buffer.toString()
    }

    private fun renderNodes(
        nodes: List<SiluetNode>,
        renderContext: RenderContext,
        evaluationContext: EvaluationContext,
        evaluator: ExpressionEvaluator,
        buffer: Appendable
    ) {
        for(node in nodes) {
            renderNode(node, renderContext, evaluationContext, evaluator, buffer)
        }
    }

    private fun renderNode(
        node: SiluetNode,
        renderContext: RenderContext,
        evaluationContext: EvaluationContext,
        evaluator: ExpressionEvaluator,
        buffer: Appendable
    ) {
        when (node) {
            is TextNode -> buffer.append(node.text)

            is PrintNode -> {
                val rawValue = evaluator.evaluate(node.expression)
                val filteredValue = evaluator.applyFilters(rawValue, node.filters, renderContext)
                val escapedValue = HTMLEscape.escape(filteredValue)

                if(filteredValue != null) {
                    buffer.append(escapedValue)
                }
            }

            is IfNode -> {
                val conditionResult = evaluator.isTruthy(evaluator.evaluate((node.condition)))
                if(conditionResult) {
                    renderNodes(node.thenBody, renderContext, evaluationContext, evaluator, buffer)
                } else if(node.elseBody != null) {
                    renderNodes(node.elseBody, renderContext, evaluationContext, evaluator, buffer)
                }
            }

            is ForNode -> {
                val collection = evaluator.evaluate(node.collectionExpression)
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

                evaluationContext.pushScope()
                try {
                    var iterations = 0

                    for (item in items) {
                        if(++iterations > maxLoopIterations) {
                            throw LoopLimitExceededException(
                                maxLoopIterations,
                                sourceLocation = node.sourceLocation
                            )
                        }

                        evaluationContext.setLocal(node.variableName, item)
                        renderNodes(node.body, renderContext, evaluationContext, evaluator, buffer)
                    }
                } finally {
                    evaluationContext.popScope()
                }
            }

            is ComponentNode -> {
                val componentName = evaluator.evaluate(node.componentName)?.toString()
                    ?: throw RenderException(
                        "Component name evaluated to null",
                        sourceLocation = node.sourceLocation
                    )

                val properties = node.properties.mapValues {
                   evaluator.evaluate(it.value)
                }

                evaluationContext.pushScope()
                try {
                    for ((key, value) in properties) {
                        evaluationContext.setLocal(key, value)
                    }

                    val componentView = try {
                        viewsLoader.getOrCompileComponentView(componentName)
                    } catch (e: ComponentViewNotFoundException) {
                        throw RenderException(
                            e.message!!,
                            sourceLocation = node.sourceLocation
                        )
                    }
                    renderNodes(
                        nodes = componentView.ast,
                        renderContext = renderContext,
                        evaluationContext = evaluationContext,
                        evaluator = evaluator,
                        buffer = buffer
                    )

                } finally {
                    evaluationContext.popScope()
                }
            }

            is SectionsNode -> {
                val sectionsGroupName = if(node.sectionsGroupName != null) {
                    evaluator.evaluate(node.sectionsGroupName)?.toString()
                        ?: throw RenderException(
                            "Sections group name evaluated to null",
                            sourceLocation = node.sourceLocation
                        )
                } else {
                    null
                }

                val sections = if(sectionsGroupName != null) {
                    try{
                        sectionsGroupsLoader.getOrCompileSectionsGroup(sectionsGroupName).sections
                    } catch (e: SectionsGroupNotFoundException) {
                        throw RenderException(
                            e.message!!,
                            sourceLocation = node.sourceLocation
                        )
                    }
                } else {
                    evaluationContext.template.sections
                }

                for(section in sections) {
                    val sectionView = try {
                        viewsLoader.getOrCompileSectionView(section.name)
                    } catch (e: SectionViewNotFoundException) {
                        throw RenderException(
                            e.message!!,
                            sourceLocation = node.sourceLocation
                        )
                    }

                    evaluationContext.pushScope()
                    evaluationContext.setLocal("section", section)
                    try{
                        this.renderNodes(
                            nodes = sectionView.ast,
                            renderContext = renderContext,
                            evaluationContext = evaluationContext,
                            evaluator = evaluator,
                            buffer = buffer
                        )
                    } finally {
                        evaluationContext.popScope()
                    }
                }
            }

            is PaginateNode -> {
                val rawCollection = evaluator.evaluate(node.collectionExpression)
                val variableName = node.variableName

                val rawPageSize = evaluator.evaluate(node.pageSizeExpression)

                val pageSize = (rawPageSize as? Number)?.toInt()?.coerceAtLeast(1) ?: 10

                val currentPage = if (node.pageNumberExpression != null){
                    evaluator.evaluate(node.pageNumberExpression) as? Int
                        ?: throw RenderException(
                            "Page must be a integer",
                            sourceLocation = node.pageNumberExpression.sourceLocation
                        )
                } else {
                    //TODO are we sure that we want to left this?
                    (evaluationContext.resolveVariable("currentPage") as? Number)?.toInt()
                        ?: (evaluationContext.resolveVariable("pageNumber") as? Number)?.toInt()
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

                evaluationContext.pushScope()
                try {
                    evaluationContext.setLocal(variableName, paginationContext)
                    renderNodes(node.body, renderContext, evaluationContext, evaluator, buffer)
                } finally {
                    evaluationContext.popScope()
                }
            }

            is TranslationNode -> {
                val translationKey = evaluator.evaluate(node.translationKeyExpression)
                    ?: throw RenderException(
                        "Translation Key evaluated to null",
                        sourceLocation = node.sourceLocation
                    )

                if(translationKey !is String) {
                    throw RenderException(
                        "Translation key must be a string",
                        sourceLocation = node.sourceLocation
                    )
                }

                val translation = try{
                     localeRegistry.getTranslation(renderContext.locale, translationKey)
                } catch (e: LocaleNotFoundException) {
                    throw RenderException(
                        e.message!!,
                        sourceLocation = node.sourceLocation
                    )
                }

                val escapedTranslation = HTMLEscape.escape(translation)

                buffer.append(escapedTranslation)
            }

            is DirectiveNode -> {
                val directive = evaluationContext.directives[node.name]
                    ?: throw RenderException(
                        "Unknown directive: @${node.name}",
                        sourceLocation = node.sourceLocation
                    )

                val args = node.arguments.map { evaluator.evaluate(it) }
                val namedArgs = node.namedArguments.mapValues { evaluator.evaluate(it.value) }
                val bodySupplier: (() -> String)? = node.body?.let { bodyNodes -> {
                    val bodyBuffer = StringBuffer()
                    renderNodes(bodyNodes, renderContext, evaluationContext, evaluator, bodyBuffer)
                    bodyBuffer.toString()
                }}

                directive.execute(args, namedArgs, bodySupplier, renderContext, buffer)
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