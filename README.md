# 成都地铁列车位置实时大屏 (H5 复刻版)

本工程像素级复刻成都地铁官方 App 实时列车位置与到站信息功能。

## 目录结构

- `index.html`: 单页面完整应用（Vue 3 + Tailwind CSS）
- `server.py`: 本地 Python 开发代理服务器（解决跨域）
- `start.bat`: 本地一键启动脚本
- `vercel.json`: **Vercel 一键免服务器部署配置**（内置 Edge 反向代理）
- `_worker.js`: **Cloudflare Pages 一键部署 Edge 代理脚本**
- `_redirects`: **Netlify 一键反向代理配置**
- `data/`: 离线站点与站间距缓存兜底数据

---

## 免费上线部署指南

### 方式一：Vercel（推荐，100% 免费）

1. **GitHub 导入部署**：
   - 将本项目代码上传到你的 GitHub 仓库。
   - 登录 [Vercel 控制台](https://vercel.com/)，点击 **Add New...** -> **Project**。
   - 导入该仓库，**Framework Preset** 选择 **Other**（无需任何构建命令）。
   - 点击 **Deploy**，约 10 秒即可生成公网访问链接。
   - `vercel.json` 会自动将 `/api/*` 请求通过 Edge 反向代理到成都地铁官方接口，无需维护任何后端！

2. **CLI 终端一键部署**：
   ```bash
   npm i -g vercel
   cd cdmetro-train-position-h5
   vercel --prod
   ```

> 💡 **国内网络访问贴士**：Vercel 默认的 `*.vercel.app` 域名在部分国内运营商网络下可能有波动。建议在 Vercel 的 `Settings -> Domains` 中绑定一个自己的域名，全国秒开。

---

### 方式二：Cloudflare Pages（国内直连极佳，100% 免费）

1. 登录 [Cloudflare Dashboard](https://dash.cloudflare.com/) -> **Workers & Pages** -> **Create application** -> **Pages**。
2. 绑定 GitHub 仓库直接导入，或者在 **Upload assets** 中直接将文件夹打包上传。
3. 根目录的 `_worker.js` 会自动作为 Cloudflare Edge 代理官方数据。

---

### 方式三：Netlify（拖拽即上线）

1. 登录 [Netlify](https://app.netlify.com/)。
2. 将 `cdmetro-train-position-h5` 文件夹直接拖拽到 Netlify 的 **Sites** 区域即可自动上线。
