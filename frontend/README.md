# Frontend Demo

## 启动方式

```bash
npm install
npm run dev
```

默认访问：`http://localhost:5173`。

## 后端地址配置

前端默认通过 Vite 代理 `/api` 到后端：

- 配置文件：`frontend/vite.config.ts`
- 默认目标：`http://localhost:8080`

如需修改后端地址，请调整 `vite.config.ts` 中的 `server.proxy` 配置。

## SSE 注意事项

- SSE 需要保持长连接，建议在开发环境使用前端同源路径 `/api/...`。
- 如果出现断流或连接失败，请确认：
  - 后端正在运行且端口可访问
  - 浏览器没有拦截混合内容（HTTP/HTTPS）
  - 本地代理没有被禁用
