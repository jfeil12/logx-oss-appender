# .ide/Dockerfile
FROM docker.cnb.cool/foobar-ai/ide-template/idea-jdk8-ai

WORKDIR /root 

# npm镜像加速
RUN sed -i 's/deb.debian.org/mirrors.cloud.tencent.com/g' /etc/apt/sources.list.d/debian.sources && \
    sed -i 's/security.debian.org/mirrors.cloud.tencent.com/g' /etc/apt/sources.list.d/debian.sources && \
    npm config set registry https://mirrors.cloud.tencent.com/npm/

ENV LANG C.UTF-8