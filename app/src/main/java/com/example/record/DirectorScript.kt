package com.example.record

/**
 * JavaScript injected into the live page. It finds elements described by an ElementSpec and performs the
 * action visibly (smooth scroll, gliding cursor, real typing events, click ripple) so the WebView capture
 * shows exactly what happens. Results are reported through the `CCBridge` JavaScript interface.
 */
object DirectorScript {
    const val JS = """
(function(){
  if (window.__cc) return;
  var cur = document.createElement('div');
  cur.id = '__cc_cursor';
  cur.style.cssText = 'position:fixed;left:0;top:0;width:28px;height:28px;z-index:2147483647;pointer-events:none;display:none;will-change:transform;';
  cur.innerHTML = '<svg width="28" height="28" viewBox="0 0 24 24"><path d="M3 2l7 18 2.6-7.4L20 10z" fill="#fff" stroke="#111" stroke-width="1.4"/></svg>';
  (document.body || document.documentElement).appendChild(cur);
  var cx = window.innerWidth * 0.8, cy = window.innerHeight * 0.85;
  function sleep(ms){ return new Promise(function(r){ setTimeout(r, ms); }); }
  function norm(s){ return (s || '').toLowerCase().replace(/\s+/g, ' ').trim(); }
  function visible(el){
    var r = el.getBoundingClientRect(), s = getComputedStyle(el);
    return r.width > 2 && r.height > 2 && s.visibility !== 'hidden' && s.display !== 'none' && s.opacity !== '0';
  }
  function labelOf(el){
    var t = '';
    if (el.labels && el.labels.length) t = el.labels[0].textContent;
    else { var p = el.closest && el.closest('label'); if (p) t = p.textContent; }
    return norm(t);
  }
  function candidates(kind){
    var sel = kind === 'input' ? 'input,textarea,select' : 'button,a,[role=button],input[type=submit],input[type=button]';
    return [].slice.call(document.querySelectorAll(sel)).filter(visible);
  }
  function labelRaw(el){
    var t = '';
    if (el.labels && el.labels.length) t = el.labels[0].textContent;
    else { var p = el.closest && el.closest('label'); if (p) t = p.textContent; }
    if (!t) t = el.getAttribute('aria-label') || '';
    return (t || '').replace(/\s+/g, ' ').trim().slice(0, 60);
  }
  function find(spec){
    var list = candidates(spec.kind);
    if (spec.index !== undefined && spec.index >= 0 && list[spec.index]) {
      var c = list[spec.index];
      var okName = !spec.name || c.getAttribute('name') === spec.name;
      var okId = !spec.id || c.id === spec.id;
      if (okName && okId) return c;
    }
    var best = null, bs = 0;
    list.forEach(function(el){
      var sc = 0;
      if (spec.kind === 'input') {
        var type = (el.getAttribute('type') || el.tagName).toLowerCase();
        if (['hidden','submit','button','checkbox','radio','file'].indexOf(type) >= 0 && spec.type !== type) return;
        if (spec.name && el.getAttribute('name') === spec.name) sc += 6;
        if (spec.id && el.id === spec.id) sc += 6;
        if (spec.placeholder && norm(el.getAttribute('placeholder')) === norm(spec.placeholder)) sc += 5;
        var lab = labelOf(el), want = norm(spec.label);
        if (want && lab && (lab.indexOf(want) >= 0 || want.indexOf(lab) >= 0)) sc += 4;
        if (want && norm(el.getAttribute('aria-label')).indexOf(want) >= 0) sc += 4;
        if (spec.type && type === spec.type) sc += 2;
      } else {
        var txt = norm(el.textContent || el.value || el.getAttribute('aria-label')), w = norm(spec.text);
        if (w && txt === w) sc += 6; else if (w && txt && (txt.indexOf(w) >= 0 || w.indexOf(txt) >= 0)) sc += 3;
        if (spec.id && el.id === spec.id) sc += 6;
      }
      if (sc > bs) { bs = sc; best = el; }
    });
    if (best && bs >= 3) return best;
    if (spec.kind === 'input' && ['password','email','tel','search'].indexOf(spec.type) >= 0) {
      var same = list.filter(function(e){ return (e.getAttribute('type') || '').toLowerCase() === spec.type; });
      if (same.length === 1) return same[0];
    }
    return null;
  }
  async function moveTo(el){
    el.scrollIntoView({block: 'center', inline: 'nearest', behavior: 'smooth'});
    await sleep(650);
    var r = el.getBoundingClientRect();
    var tx = r.left + Math.min(r.width * 0.6, r.width - 6), ty = r.top + r.height / 2;
    cur.style.display = 'block';
    var sx = cx, sy = cy, t0 = performance.now(), dur = 750;
    await new Promise(function(done){
      (function step(now){
        var p = Math.min(1, (now - t0) / dur), e = p * p * (3 - 2 * p);
        cx = sx + (tx - sx) * e; cy = sy + (ty - sy) * e;
        cur.style.transform = 'translate(' + cx + 'px,' + cy + 'px)';
        if (p < 1) requestAnimationFrame(step); else done();
      })(t0);
    });
  }
  function ripple(){
    var d = document.createElement('div');
    d.style.cssText = 'position:fixed;z-index:2147483646;pointer-events:none;border-radius:50%;border:4px solid rgba(56,189,248,.9);width:16px;height:16px;left:' + (cx - 8) + 'px;top:' + (cy - 8) + 'px;transition:all .6s ease-out;opacity:1;';
    document.documentElement.appendChild(d);
    requestAnimationFrame(function(){ d.style.transform = 'scale(4.5)'; d.style.opacity = '0'; });
    setTimeout(function(){ d.remove(); }, 700);
  }
  function highlight(el){
    var o = el.style.outline, oo = el.style.outlineOffset, tr = el.style.transition;
    el.style.transition = 'outline-color .2s'; el.style.outline = '3px solid #6366f1'; el.style.outlineOffset = '3px';
    return function(){ el.style.outline = o; el.style.outlineOffset = oo; el.style.transition = tr; };
  }
  function setValue(el, v){
    var proto = el.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
    var d = Object.getOwnPropertyDescriptor(proto, 'value');
    if (d && d.set) d.set.call(el, v); else el.value = v;
    el.dispatchEvent(new Event('input', {bubbles: true}));
  }
  function safeLink(el){
    if (el.tagName !== 'A' || !el.href) return false;
    return /^https?:/i.test(el.href) && el.target !== '_blank' && !el.hasAttribute('download');
  }
  window.__cc = {
    probe: function(spec){ return !!find(spec); },
    scan: function(){
      var out = [];
      ['input', 'button'].forEach(function(kind){
        candidates(kind).forEach(function(el, i){
          var type = (el.getAttribute('type') || el.tagName).toLowerCase();
          var text = (el.textContent || el.value || '').replace(/\s+/g, ' ').trim().slice(0, 80);
          var lab = kind === 'input' ? labelRaw(el) : (el.getAttribute('aria-label') || '');
          if (kind === 'button' && !text && !lab && !el.id) return;
          out.push({kind: kind, tag: el.tagName.toLowerCase(), type: type, name: el.getAttribute('name') || '', id: el.id || '',
            placeholder: el.getAttribute('placeholder') || '', label: lab, text: kind === 'button' ? (text || lab) : '',
            index: i, inForm: !!(el.closest && el.closest('form')), required: !!el.required});
        });
      });
      return JSON.stringify({title: document.title, url: location.href, elements: out});
    },
    run: async function(token, a){
      var res = {ok: false, found: false, clicked: false}, sent = false;
      function post(){ if (sent) return; sent = true; try { CCBridge.done(token, JSON.stringify(res)); } catch (e) {} }
      try {
        if (a.op === 'wait') { await sleep(a.ms || 800); res.ok = true; }
        else {
          var el = find(a.spec);
          if (!el) { res.found = false; }
          else {
            res.found = true;
            await moveTo(el);
            var undo = highlight(el);
            if (a.op === 'type') {
              el.focus(); ripple(); await sleep(250);
              var txt = a.text || '', cur0 = '';
              if (el.tagName === 'SELECT') { }
              else for (var i = 0; i < txt.length; i++) { cur0 += txt[i]; setValue(el, cur0); await sleep(75); }
              await sleep(400);
            } else {
              await sleep(300); ripple(); await sleep(350);
              if (a.real || safeLink(el)) { res.clicked = true; res.ok = true; post(); el.click(); }
              await sleep(500);
            }
            undo();
            res.ok = true;
          }
        }
      } catch (e) { res.error = String(e); }
      post();
    }
  };
})();
"""
}
