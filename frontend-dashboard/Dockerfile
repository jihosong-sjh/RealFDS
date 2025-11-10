# Multi-stage build: 빌드 단계와 실행 단계 분리

# Stage 1: 빌드 단계 (Node.js로 React 앱 빌드)
FROM node:20-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# package.json과 package-lock.json 복사 (의존성 캐싱 최적화)
COPY package*.json ./

# 의존성 설치
RUN npm ci --only=production=false

# 소스 코드 복사
COPY . .

# 프로덕션 빌드 (dist/ 디렉토리에 생성)
RUN npm run build

# Stage 2: 실행 단계 (Nginx로 정적 파일 서빙)
FROM nginx:alpine

# 빌드된 파일을 Nginx 정적 파일 디렉토리로 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# 커스텀 Nginx 설정 파일 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 포트 8083 노출
EXPOSE 8083

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]
