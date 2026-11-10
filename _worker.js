export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname.startsWith('/api/')) {
      const targetUrl = 'https://app.cdmetro.chengdurail.cn/op/' + url.pathname.slice(5) + url.search;
      const response = await fetch(targetUrl, {
        method: request.method,
        headers: {
          'Accept': 'application/json',
          'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)'
        }
      });
      const newHeaders = new Headers(response.headers);
      newHeaders.set('Access-Control-Allow-Origin', '*');
      return new Response(response.body, {
        status: response.status,
        headers: newHeaders
      });
    }
    return env.ASSETS.fetch(request);
  }
};
