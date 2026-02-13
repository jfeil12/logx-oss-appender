# .ide/Dockerfile
FROM docker.cnb.cool/foobar-ai/ide-template/idea-jdk8-ai:latest

WORKDIR /root 

# npm镜像加速
RUN sed -i 's/deb.debian.org/mirrors.cloud.tencent.com/g' /etc/apt/sources.list.d/debian.sources && \
    sed -i 's/security.debian.org/mirrors.cloud.tencent.com/g' /etc/apt/sources.list.d/debian.sources && \
    npm config set registry https://mirrors.cloud.tencent.com/npm/
# 设置上游仓库
RUN git remote add upstream https://github.com/foobar-ai/logx-oss-appender.git
ENV LANG C.UTF-8