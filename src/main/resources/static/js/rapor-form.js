(function () {
    var findings = document.getElementById('findingsInput');
    if (!findings) {
        return;
    }
    var mirror = document.getElementById('hlMirror');

    // Başlıklar ve sırası, sunucudaki FindingsHeading enum'uyla aynı olmalıdır.
    var LABELS = ['Olağan Dışı Bulgular', 'İzlenmemiştir', 'Saptanmamıştır', 'Açıktır', 'Doğaldır'];
    var ABNORMAL_ALIASES = ['olagan disi bulgular', 'olagan disi', 'bulgular'];

    function fold(text) {
        return text.toLocaleLowerCase('tr').replace(/ı/g, 'i').replace(/ş/g, 's').replace(/ğ/g, 'g')
            .replace(/ü/g, 'u').replace(/ö/g, 'o').replace(/ç/g, 'c').replace(/\s+/g, ' ').trim();
    }

    function headingIndex(name) {
        var key = fold(name);
        if (ABNORMAL_ALIASES.indexOf(key) !== -1) {
            return 0;
        }
        for (var i = 1; i < LABELS.length; i++) {
            if (fold(LABELS[i]) === key) {
                return i;
            }
        }
        return -1;
    }

    function escapeHtml(text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    // Başlıklar yerinde, sırada ve her biri tek kez mi? (Doktor yalnızca başlıkların yanına yazar.)
    function structureOk(text) {
        var lines = text.split('\n');
        var next = 0;
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];
            if (next < LABELS.length && line.indexOf(LABELS[next] + ':') === 0) {
                if (next > 0 && lines[i - 1] !== '') {
                    return false;
                }
                next++;
                continue;
            }
            if (i === 0) {
                return false;
            }
            var m = line.match(/^\s*([^:]{1,40}):/);
            if (m && headingIndex(m[1]) !== -1) {
                return false;
            }
        }
        return next === LABELS.length;
    }

    function renderMirror() {
        mirror.innerHTML = findings.value.split('\n').map(function (line) {
            var m = line.match(/^(\s*)([^:]{1,40}:)(.*)$/);
            var isHeading = m && headingIndex(m[2].slice(0, -1)) !== -1;
            return isHeading
                ? escapeHtml(m[1]) + '<mark>' + escapeHtml(m[2]) + '</mark>' + escapeHtml(m[3])
                : escapeHtml(line);
        }).join('\n') + '\n';
        findings.style.height = 'auto';
        findings.style.height = Math.max(420, findings.scrollHeight + 2) + 'px';
    }

    // Yapıyı bozan her değişiklik geri alınır.
    var previousText = '';
    var previousSelection = [0, 0];
    findings.addEventListener('beforeinput', function () {
        previousText = findings.value;
        previousSelection = [findings.selectionStart, findings.selectionEnd];
    });
    findings.addEventListener('input', function () {
        if (!structureOk(findings.value)) {
            findings.value = previousText;
            findings.setSelectionRange(previousSelection[0], previousSelection[1]);
        }
        renderMirror();
    });

    renderMirror();
})();