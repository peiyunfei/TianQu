---
name: publish-tianqu
description: 天衢框架维护者内部使用的发布技能，用于更新版本号、打包中央仓库发布产物并引导手动上传到 Maven Central Portal。
version: 1.0.0
---

# 天衢发布技能 (publish-tianqu)

> ⚠️ 这是仓库维护者内部使用的发布技能，不属于对外分发的接入方组件。

## 触发方式

用户输入包含以下任意关键词时激活：
- 发布天衢
- 上传中央仓库
- publish tianqu
- release tianqu
- 发布新版本

## 工作流

### 第 1 步：确认发布意图与更新版本号

询问用户以下信息再继续，不可自行假设：

1. 本次要发布的版本号是多少？（例如：1.0.6）
2. 确认三个模块都需要一起发布吗？（router-annotations、router-processor、router-runtime）

**等待用户明确回复后，读取 `gradle.properties`。**

找到以下三个字段：

```
TIANQU_ANNOTATIONS_VERSION
TIANQU_PROCESSOR_VERSION
TIANQU_RUNTIME_VERSION
```

将三个字段的值全部更新为用户指定的版本号。

更新完成后，展示 差异 给用户确认，等待用户明确同意后，再执行后续步骤。

---

### 第 2 步：清理历史产物并执行打包命令

先清理上次打包可能遗留的产物：
```bash
rm -rf build/central-bundle build/tianqu-release.zip
```

然后执行打包：
```bash
./gradlew publishAllPublicationsToCentralPortalBundleRepository
```

该命令会将所有模块的构件（pom、jar、aar、javadoc、sources 及对应 .asc 签名文件）打包到 `build/central-bundle/`。

等待命令执行完成，如果失败立即停止并告知用户错误信息。

---

### 第 3 步：压缩产物为 ZIP

```bash
cd build/central-bundle && zip -r ../tianqu-release.zip .
```

> 只有在第 2 步完成清理后才执行压缩，避免旧版本文件被一起打进 ZIP。

执行完成后确认 `build/tianqu-release.zip` 文件存在，并告知用户文件大小。

---

### 第 4 步：提示用户手动上传

由于 Maven Central Portal 上传必须通过浏览器操作，此步骤需要用户手动完成。

告知用户以下操作步骤：

1. 打开浏览器，访问 https://central.sonatype.com/publishing 并登录账号
2. 点击右上角的 **"Publish"** 按钮
3. 点击 **"Upload a deployment bundle"**
4. 填写发布信息：
   - **Deployment Name**：填写本次版本，例如 `TianQu 1.0.6`
   - **File**：选择 `build/tianqu-release.zip`
5. 点击 **"Upload"**

---

### 第 5 步：提醒用户验证与发布

提醒用户上传完成后，在 Portal 页面：

1. 等待状态从 **"Validating"** 变为 **"Validated"**（可能需要几分钟）
2. 点击对应记录，再点击右上角 **"Publish"** 按钮
3. 确认发布，状态会变为 **"Publishing"**，随后变为 **"Published"**

---

### 第 6 步：询问是否需要收尾工作

询问用户是否还需要：

- 更新 README.md 里的版本号引用
- 打 git tag（例如 `v1.0.6`）
- 更新 `version.json` 和 `tianqu-version.json` 并重新编译 MCP Server JAR

等待用户决定，按需执行。

---

## 注意事项

- **绝对不能**在没有用户明确确认的情况下自行修改版本号
- 第 4、5 步必须由用户手动完成，不要尝试自动化浏览器操作
- 如果 `./gradlew` 命令失败，停止流程，不要跳过
- 打包成功后告知用户 ZIP 文件的完整路径，方便用户在 Finder 中定位
