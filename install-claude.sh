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

INSTALLER_VERSION="1.0.0"
GITEE_RAW="https://gitee.com/zhongte/TianQu/raw/master"
SKILL_URL="${GITEE_RAW}/.claude/skills/tianqu/SKILL.md"
JAR_URL="${GITEE_RAW}/bin/tianqu-mcp-server-all.jar"

SKILL_DIR=".claude/skills/tianqu"
MCP_DIR=".claude/mcp"

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

# ---------- 注册 MCP Server ----------
echo ""
echo -e "${YELLOW}🔧 注册 MCP Server（project 作用域）...${NC}"
if claude mcp add tianqu --scope project -- java -jar ".claude/mcp/tianqu-mcp-server-all.jar" 2>&1; then
    echo "   ✅ tianqu MCP Server 已注册到项目 .claude/settings.json"
else
    echo -e "${YELLOW}   ⚠️  MCP 注册命令返回非零状态，请检查上方输出。${NC}"
    echo "   您也可以手动在项目 .claude/settings.json 的 mcpServers 节点中添加："
    echo '   "tianqu": { "command": "java", "args": ["-jar", ".claude/mcp/tianqu-mcp-server-all.jar"] }'
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
echo ""
