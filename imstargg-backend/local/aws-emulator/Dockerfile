# AWS Emulator 는 공식 이미지를 배포하지 않아 공식 문서의 Dockerfile 로 직접 빌드한다.
# https://docs.aws.amazon.com/emulator/latest/userguide/getting-started-docker.html
# 버전 목록: https://downloads.aws.com/aws-emulator/packages/aws-emulator/LTS/versions.json
FROM public.ecr.aws/amazonlinux/amazonlinux:2023-minimal

ARG AWS_EMULATOR_VERSION=1.0.338
ARG TARGETARCH

RUN case "${TARGETARCH}" in \
        amd64) RPM_ARCH=x86_64 ;; \
        arm64) RPM_ARCH=aarch64 ;; \
        *) echo "지원하지 않는 아키텍처: ${TARGETARCH}" >&2; exit 1 ;; \
    esac \
    && curl -fsSL -o /tmp/aws-emulator.rpm \
        "https://downloads.aws.com/aws-emulator/packages/aws-emulator/LTS/aws-emulator-${AWS_EMULATOR_VERSION}-1-${RPM_ARCH}.rpm" \
    && rpm -i /tmp/aws-emulator.rpm && rm /tmp/aws-emulator.rpm \
    && mkdir -p /home/emulator && chown 1000 /home/emulator

USER 1000
ENV HOME=/home/emulator
EXPOSE 9297
ENTRYPOINT ["aws-emulator", "-b", "0.0.0.0"]
