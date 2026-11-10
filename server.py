import http.server
import socketserver
import urllib.request
import urllib.parse
import ssl
import json
import os
import sys

PORT = 8088
DIRECTORY = os.path.dirname(os.path.abspath(__file__))

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

HEADERS = {
    'User-Agent': 'okhttp/3.14.9',
    'Accept': 'application/json'
}

class MetroProxyHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        query = parsed.query

        if path.startswith('/api/'):
            self.handle_api(path, query)
        else:
            super().do_GET()

    def handle_api(self, path, query):
        target_path = path.replace('/api/', 'op/')
        target_url = f"https://app.cdmetro.chengdurail.cn/{target_path}"
        if query:
            target_url += f"?{query}"

        print(f"[API PROXY] -> {target_url}")
        try:
            req = urllib.request.Request(target_url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=8, context=ctx) as resp:
                data = resp.read()
                self.send_response(200)
                self.send_header('Content-Type', 'application/json; charset=utf-8')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(data)
                return
        except Exception as e:
            print(f"[API ERROR] {e}, falling back to local cached data...")

        # Fallback to local files if API fails or offline
        fallback_data = self.get_fallback(path, query)
        self.send_response(200)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(json.dumps(fallback_data, ensure_ascii=False).encode('utf-8'))

    def get_fallback(self, path, query):
        qs = urllib.parse.parse_qs(query)
        line_no = qs.get('lineNo', ['02'])[0]

        if 'stations' in path:
            local_file = os.path.join(DIRECTORY, 'data', 'stations_all.json')
            if os.path.exists(local_file):
                with open(local_file, 'r', encoding='utf-8') as f:
                    return json.load(f)

        if 'station-distance' in path:
            local_file = os.path.join(DIRECTORY, 'data', f'distances_{line_no}.json')
            if os.path.exists(local_file):
                with open(local_file, 'r', encoding='utf-8') as f:
                    return json.load(f)

        return {"code": 0, "msg": "fallback", "data": {}}

def run():
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), MetroProxyHandler) as httpd:
        print(f"==================================================")
        print(f"  成都地铁【列车位置】H5 服务已启动!")
        print(f"  访问地址: http://127.0.0.1:{PORT}")
        print(f"  按 Ctrl+C 停止服务")
        print(f"==================================================")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n服务已停止")

if __name__ == '__main__':
    run()
