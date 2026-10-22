/* Rapor ekranları (rapor-olustur.html, rapor-detay.html) için ortak mockup mantığı.
   Cümle kalıpları ve sınıflandırma kuralları, gerçek uygulamada Java servis
   katmanında çalışacak davranışın prototipidir. */

// Bölüm başlıkları aynı zamanda çıktı sırasıdır: olağan dışı -> olumsuzlamalar -> normal.
const PREDICATES = [
  { key: "izlenmemis",  label: "İzlenmemiştir",  predicate: "izlenmemiştir",  hint: "ör. ekstra-aksiyel koleksiyon, orta hat şifti" },
  { key: "saptanmamis", label: "Saptanmamıştır", predicate: "saptanmamıştır", hint: "ör. patolojik dansite değişikliği" },
  { key: "acik",        label: "Açıktır",        predicate: "açıktır",        hint: "ör. bazal sisternalar, kortikal sulkuslar" },
  { key: "dogal",       label: "Doğaldır",       predicate: "doğaldır",       hint: "ör. ventriküler sistem, kranyum kemik yapıları" }
];

// Tek metin alanındaki başlıklar. Sıra, metin alanına konan iskeletin sırasıdır.
// Eşleştirme Türkçe karakterlerden bağımsızdır ("izlenmemistir" de tanınır).
const HEADINGS = [
  { key: "abnormal", label: "Olağan Dışı Bulgular", aliases: ["olagan disi bulgular", "olagan disi", "bulgular"] },
  ...PREDICATES.map(p => ({ key: p.key, label: p.label, aliases: [fold(p.label)] }))
];

// V3 tohum verisindeki sonuç kategorileri ve onaylı cümleler (anahtar kelimeler sadece mockup simülasyonu içindir).
const RESULT_CATEGORIES = [
  { name: "Normal Bulgu", urgency: "RUTİN", keywords: [], sentence: "İncelemede akut patolojik bulgu saptanmamıştır." },
  { name: "Akut Kanama", urgency: "KRİTİK", keywords: ["kanama", "hemoraji"], sentence: "Akut kanama ile uyumlu bulgular izlenmiştir. Acil klinik değerlendirme önerilir." },
  { name: "Akut İskemi", urgency: "KRİTİK", keywords: ["iskemi", "infarkt"], sentence: "Akut iskemik değişiklikler ile uyumlu bulgular izlenmiştir. Acil klinik değerlendirme önerilir." },
  { name: "Pnömotoraks", urgency: "KRİTİK", keywords: ["pnömotoraks"], sentence: "Pnömotoraks ile uyumlu bulgular izlenmiştir. Acil klinik değerlendirme önerilir." },
  { name: "Pulmoner Emboli", urgency: "KRİTİK", keywords: ["emboli"], sentence: "Pulmoner emboli ile uyumlu dolum defekti izlenmiştir. Acil klinik değerlendirme önerilir." },
  { name: "Serbest Hava", urgency: "KRİTİK", keywords: ["serbest hava", "perforasyon"], sentence: "Batında serbest hava izlenmiştir; perforasyon açısından acil klinik değerlendirme önerilir." },
  { name: "Kırık", urgency: "ACİL", keywords: ["kırık", "fraktür"], sentence: "Kırık ile uyumlu bulgular izlenmiştir. Ortopedi değerlendirmesi önerilir." },
  { name: "Pnömoni", urgency: "ACİL", keywords: ["pnömoni", "konsolidasyon"], sentence: "Konsolidasyon alanları izlenmiştir; pnömoni açısından klinik korelasyon önerilir." },
  { name: "Barsak Tıkanıklığı", urgency: "ACİL", keywords: ["obstrüksiyon", "ileus", "tıkanıklık"], sentence: "Barsak obstrüksiyonu ile uyumlu bulgular izlenmiştir. Cerrahi değerlendirme önerilir." },
  { name: "İleri İnceleme Önerisi", urgency: "RUTİN", keywords: ["ileri", "kontrol", "korelasyon"], sentence: "Saptanan lezyon için ileri görüntüleme ve klinik korelasyon önerilir." }
];

const URGENCY_CLASS = { "KRİTİK": "badge-kritik", "ACİL": "badge-acil", "RUTİN": "badge-rutin", "BELİRSİZ": "badge-belirsiz" };
const URGENCY_RANK = { "RUTİN": 1, "ACİL": 2, "KRİTİK": 3 };

const SOURCE_LABEL = {
  "yz": "YZ ile yeniden yazıldı",
  "yz-bekliyor": "Kaydedilince YZ ile yeniden yazılır",
  "kalip": "Kalıpla üretildi",
  "onayli": "Onaylı sonuç cümlesi"
};

function $(id) { return document.getElementById(id); }

// Türkçe büyük/küçük harf dönüşümü (i/İ, ı/I) için locale zorunludur.
function lower(text) { return text.toLocaleLowerCase("tr"); }
function capFirst(text) { return text ? text.charAt(0).toLocaleUpperCase("tr") + text.slice(1) : text; }

// Türkçe karakterleri sadeleştirir: başlık eşleştirmesi için ("Saptanmamıştır" == "saptanmamistir").
function fold(text) {
  return lower(text).replace(/ı/g, "i").replace(/ş/g, "s").replace(/ğ/g, "g")
    .replace(/ü/g, "u").replace(/ö/g, "o").replace(/ç/g, "c").replace(/\s+/g, " ").trim();
}

// Tek metni başlıklara göre bölümlere ayırır. Başlıksız ilk satırlar "Olağan Dışı Bulgular"a gider;
// bilinmeyen "xxx:" satırları içerik sayılır (başlık sayılmaz).
function parseReportText(text) {
  const parts = {};
  HEADINGS.forEach(h => { parts[h.key] = []; });
  let current = "abnormal";
  (text || "").split("\n").forEach(line => {
    const m = line.match(/^\s*([^:]{1,40}):(.*)$/);
    const heading = m && HEADINGS.find(h => h.aliases.includes(fold(m[1])));
    if (heading) {
      current = heading.key;
      if (m[2].trim()) parts[current].push(m[2].trim());
    } else if (line.trim()) {
      parts[current].push(line.trim());
    }
  });
  const sections = {};
  Object.keys(parts).forEach(k => { sections[k] = parts[k].join("\n"); });
  return sections;
}

// Yeni rapor için metin alanının başlangıç iskeleti.
function skeletonText() {
  return HEADINGS.map(h => h.label + ": ").join("\n\n");
}

// Kayıtlı/yüklenen metni başlık iskeletine oturtur: tüm başlıklar sırayla ve aralarında boş satırla yer alır.
function canonicalText(text) {
  const sections = parseReportText(text);
  return HEADINGS.map(h => h.label + ": " + sections[h.key]).join("\n\n");
}

// Başlıkların yerinde ve sırada durup durmadığını denetler (başlıklar sabittir, doktor yalnızca yanlarına yazar).
// Kural: metin ilk başlıkla başlar, her başlık satır başında "Başlık:" biçiminde tam bir kez geçer,
// ilk başlık dışındakilerin önünde boş satır vardır, başlık gibi görünen başka satır yoktur.
function structureOk(text) {
  const lines = text.split("\n");
  let next = 0;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (next < HEADINGS.length && line.startsWith(HEADINGS[next].label + ":")) {
      if (next > 0 && lines[i - 1] !== "") return false;
      next++;
      continue;
    }
    if (i === 0) return false;
    const m = line.match(/^\s*([^:]{1,40}):/);
    if (m && HEADINGS.some(h => h.aliases.includes(fold(m[1])))) return false;
  }
  return next === HEADINGS.length;
}

// "a, b ,A" -> ["a", "b"]  (küçük harf, tekrarsız, sıra korunur)
function parseTerms(raw) {
  const seen = new Set();
  return (raw || "").split(/[,;\n]/).map(t => lower(t.trim())).filter(t => {
    if (!t || seen.has(t)) return false;
    seen.add(t);
    return true;
  });
}

// ["a"] -> "a", ["a","b"] -> "a ve b", ["a","b","c"] -> "a, b ve c"
function joinTr(items) {
  if (items.length === 1) return items[0];
  return items.slice(0, -1).join(", ") + " ve " + items[items.length - 1];
}

function buildSentence(terms, predicate) {
  return capFirst(joinTr(terms)) + " " + predicate + ".";
}

// Mockup'ta yapay zekanın yeniden yazımı simüle edilir (büyük harf + nokta).
function rewriteAbnormal(text) {
  return text.split(/\n+/).map(l => l.trim()).filter(Boolean)
    .map(l => capFirst(l).replace(/[.!?]*$/, "."))
    .join(" ");
}

function buildFindingsBlocks(input, saved) {
  const sections = parseReportText(input.text);
  const blocks = [];
  const abnormal = sections.abnormal.trim();
  if (abnormal) {
    blocks.push(saved
      ? { text: rewriteAbnormal(abnormal), source: "yz" }
      : { text: abnormal, source: "yz-bekliyor" });
  }
  PREDICATES.forEach(p => {
    const terms = parseTerms(sections[p.key]);
    if (terms.length) blocks.push({ text: buildSentence(terms, p.predicate), source: "kalip" });
  });
  return blocks;
}

// Doktorun kendi girdileri arasındaki çelişkiler (basit metin karşılaştırması).
function findConflicts(input) {
  const sections = parseReportText(input.text);
  const warnings = [];
  const where = new Map();
  PREDICATES.forEach(p => parseTerms(sections[p.key]).forEach(term => {
    where.set(term, [...(where.get(term) || []), p.label]);
  }));
  const abnormal = lower(sections.abnormal);
  where.forEach((labels, term) => {
    if (labels.length > 1) {
      warnings.push(`"${term}" birden fazla bölümde yazılmış: ${labels.join(", ")}.`);
    }
    if (abnormal && abnormal.includes(term)) {
      warnings.push(`"${term}" hem "${labels[0]}" bölümünde hem olağan dışı bulgularda geçiyor.`);
    }
  });
  if (input.normalTetkik && abnormal.trim()) {
    warnings.push(`"Normal tetkik" işaretli, ama olağan dışı bulgu yazılmış.`);
  }
  if (input.normalTetkik && (input.sonuc || "").trim()) {
    warnings.push(`"Normal tetkik" işaretli, ama Sonuç / Öneriler yazılmış; işaretliyse Sonuç / Öneriler yok sayılır.`);
  }
  return warnings;
}

// Sonuç / Öneriler metnini kapalı kategori kümesine eşler (gerçekte LLM sınıflandırması).
function classifyResult(input) {
  let matched;
  if (input.normalTetkik) {
    matched = [RESULT_CATEGORIES[0]];
  } else {
    const text = lower(input.sonuc || "");
    matched = RESULT_CATEGORIES.slice(1).filter(c => c.keywords.some(k => text.includes(k)));
  }
  if (!matched.length) {
    return { categories: [], sentence: "", urgency: "BELİRSİZ" };
  }
  const urgency = matched.reduce((top, c) => URGENCY_RANK[c.urgency] > URGENCY_RANK[top] ? c.urgency : top, "RUTİN");
  return { categories: matched.map(c => c.name), sentence: matched.map(c => c.sentence).join(" "), urgency };
}

// ---------- Ekrana çizim yardımcıları ----------

function renderFindings(container, blocks, emptyText) {
  container.replaceChildren();
  if (!blocks.length) {
    const p = document.createElement("p");
    p.className = "muted-text";
    p.textContent = emptyText;
    container.appendChild(p);
    return;
  }
  blocks.forEach(block => {
    const div = document.createElement("div");
    div.className = "findings-block";
    const p = document.createElement("p");
    p.textContent = block.text;
    const tag = document.createElement("span");
    tag.className = "source-tag source-" + block.source;
    tag.textContent = SOURCE_LABEL[block.source];
    div.append(p, tag);
    container.appendChild(div);
  });
}

function renderResultView(result) {
  const container = $("sonucView");
  if (!result.sentence) {
    renderFindings(container, [], "Sonuç belirlenemedi. Sonuç / Öneriler metnini düzenleyin veya \"Normal tetkik\" seçin.");
    return;
  }
  renderFindings(container, [{ text: result.sentence, source: "onayli" }], "");
  const categories = document.createElement("div");
  categories.className = "field-hint";
  categories.textContent = "Kategori: " + result.categories.join(", ");
  container.appendChild(categories);
}

function setStatusBadge(status) {
  const badge = $("statusBadge");
  badge.classList.remove("hidden", "badge-draft", "badge-approved");
  badge.classList.add(status === "ONAYLANDI" ? "badge-approved" : "badge-draft");
  badge.textContent = status === "ONAYLANDI" ? "ONAYLANDI" : "TASLAK";
}

function setUrgency(level) {
  const el = $("urgencyBadge");
  el.className = "badge " + URGENCY_CLASS[level];
  el.textContent = level;
}

function fillTetkikPanel(tetkik) {
  $("rProtokolNo").textContent = tetkik.protokolNo || "—";
  $("rTetkikTuru").textContent = tetkik.tetkikTuru;
  $("rAnatomikBolge").textContent = tetkik.anatomikBolge || "—";
  $("rTetkikTarihi").textContent = tetkik.tarih || "—";
  $("rIstemHekim").textContent = tetkik.istem || "—";
  $("rAcilServisten").textContent = tetkik.acil ? "Evet" : "Hayır";
}

// ---------- Rapor kartı (kaydedilmiş rapor görünümü) ----------

let currentReport = null;

function renderReportCard(report) {
  currentReport = report;
  const approved = report.status === "ONAYLANDI";
  const result = classifyResult(report.input);

  setStatusBadge(report.status);
  setUrgency(result.urgency);
  renderFindings($("bulgularView"), buildFindingsBlocks(report.input, true), "Bulgular girilmedi.");
  renderResultView(result);

  $("draftViewActions").classList.toggle("hidden", approved);
  $("approvedActions").classList.toggle("hidden", !approved);
  updateApproveState();
}

// Onay kuralları: Bulgular ve Sonuç dolu olmalı; KRİTİK ise bildirim bilgisi eksiksiz olmalı.
function updateApproveState() {
  if (!currentReport) return;
  const input = currentReport.input;
  const result = classifyResult(input);
  const reasons = [];
  if (!buildFindingsBlocks(input, true).length) reasons.push("Bulgular boş.");
  if (!result.sentence) reasons.push("Sonuç belirlenemedi.");
  $("btnOnayla").disabled = reasons.length > 0;
  $("approveHint").textContent = reasons.length ? "Onaylamak için: " + reasons.join(" ") : "";
}

// Mockup örnek raporu (gerçek uygulamada GET /api/reports/{id} ile gelir).
function sampleReport() {
  return {
    status: "TASLAK",
    tetkik: {
      protokolNo: "P-2026-108452", tetkikTuru: "BT", anatomikBolge: "Kranial",
      tarih: "30.09.2026 13:40", istem: "Acil Servis", acil: true
    },
    input: {
      text: [
        "Olağan Dışı Bulgular: sağ parietal bölgede hiperdens alan",
        "İzlenmemiştir: orta hat şifti",
        "Saptanmamıştır: ",
        "Açıktır: bazal sisternalar",
        "Doğaldır: ventriküler sistem, kranyum kemik yapıları"
      ].join("\n\n"),
      sonuc: "Akut kanama, nöroşirürji konsültasyonu önerilir.",
      normalTetkik: false
    }
  };
}
