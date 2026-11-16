// Проверяет, что все локальные файлы, на которые ссылаются index.html и import в JS, существуют
const fs = require('fs');
const path = require('path');

const root = 'frontend';
const missing = [];

function check(fromFile, target) {
    const resolved = path.join(path.dirname(fromFile), target);
    if (!fs.existsSync(resolved)) missing.push(`${fromFile} → ${target}`);
}

// 1. index.html: <link href="css/..."> и <script src="js/...">
const indexFile = path.join(root, 'index.html');
const html = fs.readFileSync(indexFile, 'utf8');
for (const [, target] of html.matchAll(/(?:src|href)="(?!https?:|#|\/\/)([^"]+)"/g)) {
    check(indexFile, target);
}

// 2. Все JS-модули: import ... from './...'
function jsFiles(dir) {
    return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) return jsFiles(full);
        return entry.name.endsWith('.js') ? [full] : [];
    });
}
for (const file of jsFiles(path.join(root, 'js'))) {
    const code = fs.readFileSync(file, 'utf8');
    for (const [, target] of code.matchAll(/from\s+['"](\.{1,2}\/[^'"]+)['"]/g)) {
        check(file, target);
    }
}

if (missing.length > 0) {
    console.error('Не найдены файлы:\n' + missing.join('\n'));
    process.exit(1);
}
console.log('Все ссылки на файлы фронтенда в порядке');