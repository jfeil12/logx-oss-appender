# .ide/Dockerfile
FROM node:22

WORKDIR /root

# 声明 /cache 为数据卷
VOLUME /root 

# 安装 ssh 服务，用于支持 JetBrains Gateway/vscode/cursor 等客户端连接
#RUN apt-get update && apt-get install -y wget unzip openssh-server

# 1. 安装SDKMAN依赖（wget/unzip已存在，补充curl用于下载）
# 2. 安装SDKMAN并初始化
# 3. 用SDKMAN安装Zulu JDK 8（选LTS版本，如8.0.402-zulu）
# 4. 安装其他依赖（openssh-server/maven）
# 5. 清理缓存减小镜像体积
RUN apt-get update && apt-get install -y wget unzip curl openssh-server zip

# java8 & maven
RUN bash -c "wget -qO- 'https://get.sdkman.io' | bash \
    && source \"$HOME/.sdkman/bin/sdkman-init.sh\" \
    && sdk install java 8.0.402-zulu --default \
    && apt-get install -y maven \
    && rm -rf /var/lib/apt/lists/* ~/.sdkman/tmp/*"

# 设置 JAVA_HOME 环境变量
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

# 安装 OpenCode AI & openspec
RUN npm install -g -y opencode-ai@latest @fission-ai/openspec@latest


# 安装 Playwright 和 Chromium
RUN npm install -g playwright && npx playwright install chromium --with-deps

# 安装 playwright-extra 和 puppeteer-extra-plugin-stealth
RUN npm install -g playwright-extra puppeteer-extra-plugin-stealth

# 创建 /ide_cnb 目录，用于安装 IDE，注意安装路径必须是这个，便于自动识别环境中支持哪些 ide
RUN mkdir -p /ide_cnb

# ========== 按需选择安装以下 IDE（支持安装多个，建议注释掉不需要的） ==========
# GoLand
# RUN wget https://download.jetbrains.com/go/goland-2025.2.5.tar.gz \
#     && tar -zxvf goland-2025.2.5.tar.gz -C /ide_cnb \
#     && rm goland-2025.2.5.tar.gz

# IntelliJ IDEA
RUN wget https://download.jetbrains.com/idea/ideaIU-2025.3.tar.gz \
    && tar -zxvf ideaIU-2025.3.tar.gz -C /ide_cnb \
    && rm ideaIU-2025.3.tar.gz

# PhpStorm
# RUN wget https://download.jetbrains.com/webide/PhpStorm-2025.2.5.tar.gz \
#     && tar -zxvf PhpStorm-2025.2.5.tar.gz -C /ide_cnb \
#     && rm PhpStorm-2025.2.5.tar.gz

# PyCharm
# RUN wget https://download.jetbrains.com/python/pycharm-2025.2.5.tar.gz \
#     && tar -zxvf pycharm-2025.2.5.tar.gz -C /ide_cnb \
#     && rm pycharm-2025.2.5.tar.gz


# RubyMine
# RUN wget https://download.jetbrains.com/ruby/RubyMine-2025.2.5.tar.gz \
#     && tar -zxvf RubyMine-2025.2.5.tar.gz -C /ide_cnb \
#     && rm RubyMine-2025.2.5.tar.gz

# WebStorm
# RUN wget https://download.jetbrains.com/webstorm/WebStorm-2025.2.5.tar.gz \
#     && tar -zxvf WebStorm-2025.2.5.tar.gz -C /ide_cnb \
#     && rm WebStorm-2025.2.5.tar.gz
# CLion
# RUN wget https://download.jetbrains.com/cpp/CLion-2025.2.5.tar.gz \
#     && tar -zxvf CLion-2025.2.5.tar.gz -C /ide_cnb \
#     && rm CLion-2025.2.5.tar.gz

# RustRover
# RUN wget https://download.jetbrains.com/rustrover/RustRover-2025.2.5.tar.gz \
#     && tar -zxvf RustRover-2025.2.5.tar.gz -C /ide_cnb \
#     && rm RustRover-2025.2.5.tar.gz

# Rider
# RUN wget https://download.jetbrains.com/rider/JetBrains.Rider-2025.3.0.3.tar.gz \
#     && tar -zxvf JetBrains.Rider-2025.3.0.3.tar.gz -C /ide_cnb \
#     && rm JetBrains.Rider-2025.3.0.3.tar.gz

# ========== 可选：安装 VSCode WebIDE ==========

RUN curl -fsSL https://code-server.dev/install.sh | sh \
    && code-server --install-extension cnbcool.cnb-welcome \
    && code-server --install-extension redhat.vscode-yaml \
    && code-server --install-extension orta.vscode-jest \
    && code-server --install-extension dbaeumer.vscode-eslint \
    && code-server --install-extension waderyan.gitblame \
    && code-server --install-extension mhutchie.git-graph \
    && code-server --install-extension donjayamanne.githistory

ENV LANG C.UTF-8