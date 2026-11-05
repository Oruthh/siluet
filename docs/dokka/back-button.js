window.addEventListener('DOMContentLoaded', () => {
    const navControls = document.querySelector('.navigation-controls') || document.querySelector('.navigation');
    if (navControls) {
        const link = document.createElement('a');
        link.href = '/';
        link.innerText = '← Back to Docs';
        link.style.marginRight = '12px';
        link.style.textDecoration = 'none';
        link.style.fontWeight = 'bold';
        link.style.color = 'var(--dokka-text-color, inherit)';
        navControls.prepend(link);
    }
});