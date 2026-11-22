document.getElementById('route').textContent = 'client route: ' + location.pathname;
const es = new EventSource('/api/stream'); const out = document.getElementById('sse');
es.onmessage = (e) => { out.textContent += new Date().toISOString() + '  ' + e.data + '\n'; };
