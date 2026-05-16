# CloudBase React Template

基于 React + Vite + TypeScript + CloudBase 的静态网站模板。

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env` 并填入真实的配置：

```bash
cp .env.example .env
```

编辑 `.env` 文件：

```env
VITE_ENV_ID=your-cloudbase-env-id
VITE_PUBLISHABLE_KEY=your-publishable-key
```

**获取配置：**
- 环境 ID: [CloudBase 控制台](https://console.cloud.tencent.com/tcb)
- Publishable Key: `https://tcb.cloud.tencent.com/dev?envId={your-env-id}#/env/apikey`

### 3. 启动开发服务器

```bash
npm run dev
```

### 4. 构建生产版本

```bash
npm run build
```

## 部署到 CloudBase

```bash
# 安装 CloudBase CLI
npm install -g @cloudbase/cli

# 登录
tcb login

# 部署
tcb framework deploy
```

## 技术栈

- React 19
- Vite 6
- TypeScript 5
- Tailwind CSS + DaisyUI
- CloudBase JS SDK

## 注意事项

⚠️ **不要将 `.env` 文件提交到 Git 仓库！**

`.env` 文件包含敏感信息，已在 `.gitignore` 中忽略。
