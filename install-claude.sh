#!/usr/bin/env bash
# =============================================================================
# 天衢路由框架 —— Claude Code 一键接入脚本
# 运行后将自动在当前项目中安装天衢 MCP Server 和 Skill，
# 让您可以通过 `/tianqu 帮我接入天衢框架` 一句话完成框架接入。
#
# 使用方式（在您自己项目的根目录执行）：
#   curl -fsSL https://gitee.com/zhongte/TianQu/raw/master/install-claude.sh | bash
# =============================================================================

set -e

INSTALLER_VERSION="1.0.3"
GITEE_BASE_URL="https://gitee.com/zhongte/TianQu"
GITEE_RAW="${GITEE_BASE_URL}/raw/master"
SKILL_URL="${GITEE_RAW}/.claude/skills/tianqu/SKILL.md"
JAR_URL="${GITEE_BASE_URL}/releases/download/v1.0.5/tianqu-mcp-server-all.jar"

SKILL_DIR=".claude/skills/tianqu"
MCP_DIR=".claude/mcp"
SETTINGS_FILE=".claude/settings.json"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo ""
echo -e "${CYAN}🚀 天衢路由 Claude Code 一键安装脚本 (v${INSTALLER_VERSION})${NC}"
echo "================================================"

# ---------- 检查 claude 命令 ----------
if ! command -v claude &> /dev/null; then
    echo -e "${RED}❌ 未检测到 claude 命令。请先安装 Claude Code：${NC}"
    echo "   https://docs.anthropic.com/en/docs/claude-code"
    exit 1
fi

# ---------- 检查 java 命令 ----------
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ 未检测到 java 命令（运行 MCP Server 需要 Java 17+）。${NC}"
    echo "   请先安装 JDK：https://adoptium.net"
    exit 1
fi

# ---------- 创建目录 ----------
echo ""
echo -e "${YELLOW}📁 创建目录...${NC}"
mkdir -p "${SKILL_DIR}"
mkdir -p "${MCP_DIR}"
echo "   ✅ .claude/skills/tianqu/  已就绪"
echo "   ✅ .claude/mcp/            已就绪"
echo "   ✅ .claude/settings.json   将自动写入/更新"

# ---------- 下载 SKILL.md ----------
echo ""
echo -e "${YELLOW}📄 下载 Skill 文件...${NC}"
if curl -fsSL "${SKILL_URL}" -o "${SKILL_DIR}/SKILL.md"; then
    echo "   ✅ .claude/skills/tianqu/SKILL.md 下载成功"
else
    echo -e "${RED}   ❌ SKILL.md 下载失败，请检查网络或手动下载：${SKILL_URL}${NC}"
    exit 1
fi

# ---------- 下载 JAR ----------
echo ""
echo -e "${YELLOW}📦 下载 MCP Server JAR（约 12 MB，请稍候）...${NC}"
if curl -fsSL --progress-bar "${JAR_URL}" -o "${MCP_DIR}/tianqu-mcp-server-all.jar"; then
    echo "   ✅ .claude/mcp/tianqu-mcp-server-all.jar 下载成功"
else
    echo -e "${RED}   ❌ JAR 下载失败，请检查网络或手动下载：${JAR_URL}${NC}"
    exit 1
fi

# ---------- 获取最新更新内容 ----------
UPDATE_MSG=""
if curl_out=$(curl -fsSL --max-time 3 "${GITEE_RAW}/version.json" 2>/dev/null); then
    # 用简单的 python 脚本提取 updateMessage 字段，不依赖 jq
    UPDATE_MSG=$(python3 -c "import sys, json; print(json.loads(sys.stdin.read()).get('updateMessage', ''))" <<< "$curl_out" 2>/dev/null)
fi

# ---------- 注册 MCP Server ----------
echo ""
echo -e "${YELLOW}🔧 注册 MCP Server 到项目配置...${NC}"

# 如果 settings.json 不存在，创建一个基础的空对象结构
if [ ! -f "${SETTINGS_FILE}" ]; then
    echo "{}" > "${SETTINGS_FILE}"
fi

# 使用 python3 安全地注入 MCP Server 配置到 settings.json
if python3 -c '
import json, sys

settings_file = sys.argv[1]
try:
    with open(settings_file, "r") as f:
        data = json.load(f)
except (FileNotFoundError, json.JSONDecodeError):
    data = {}

data.setdefault("mcpServers", {})
data["mcpServers"]["tianqu"] = {
    "command": "java",
    "args": ["-jar", ".claude/mcp/tianqu-mcp-server-all.jar"]
}

with open(settings_file, "w") as f:
    json.dump(data, f, indent=2)
' "${SETTINGS_FILE}" 2>/dev/null; then
    echo "   ✅ tianqu MCP Server 已成功注册到项目 ${SETTINGS_FILE}"
else
    echo -e "${YELLOW}   ⚠️  自动更新 ${SETTINGS_FILE} 失败，请手动添加 MCP 配置。${NC}"
    echo "   请手动在项目 .claude/settings.json 中添加："
    echo '   {'
    echo '     "mcpServers": {'
    echo '       "tianqu": { "command": "java", "args": ["-jar", ".claude/mcp/tianqu-mcp-server-all.jar"] }'
    echo '     }'
    echo '   }'
fi

# ---------- 完成 ----------
echo ""
echo "================================================"
echo -e "${GREEN}🎉 安装完成！现在在项目目录启动 Claude Code：${NC}"
echo ""
echo "   claude"
echo ""
echo -e "${CYAN}然后输入以下指令，让 AI 帮您一键接入天衢框架：${NC}"
echo ""
echo "   /tianqu 帮我接入天衢框架"
echo ""
echo -e "${CYAN}或者直接描述需求，例如：${NC}"
echo ""
echo "   /tianqu 天衢实现了哪些功能"
echo "   /tianqu 使用协程实现页面间通信"
echo "   /tianqu 实现栈顶复用或者栈内复用"
echo "   /tianqu 实现跨模块通信"
echo "   /tianqu 帮我用天衢写一个带预加载的商品详情页"
echo "   /tianqu 帮我配置天衢的全局 404 降级拦截"
echo ""
echo -e "${YELLOW}💡 提示：天衢支持自动检查更新。如果在后续使用中提示有新版本，再次执行此 curl 命令并重启 Claude 即可完成升级。${NC}"

if [ -n "$UPDATE_MSG" ]; then
    echo ""
    echo -e "${CYAN}✨ 本次天衢版本更新内容：${NC}"
    echo -e "   ${UPDATE_MSG}"
fi
echo ""
