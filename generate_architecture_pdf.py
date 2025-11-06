#!/usr/bin/env python3
"""
RealFDS 아키텍처 PDF 생성 스크립트
실시간 금융 거래 탐지 시스템의 전체 아키텍처를 도식화하여 PDF로 생성
"""

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle,
    Image as RLImage, KeepTogether
)
from reportlab.pdfgen import canvas
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_JUSTIFY
from reportlab.graphics.shapes import Drawing, Rect, String, Line, Polygon
from reportlab.graphics import renderPDF
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from datetime import datetime
import os
import sys


def register_korean_font():
    """한글 폰트 등록"""
    # Windows 시스템 폰트 경로
    font_paths = [
        'C:/Windows/Fonts/malgun.ttf',  # Malgun Gothic
        'C:/Windows/Fonts/gulim.ttc',   # Gulim
        'C:/Windows/Fonts/batang.ttc',  # Batang
    ]

    # Linux 시스템 폰트 경로
    font_paths.extend([
        '/usr/share/fonts/truetype/nanum/NanumGothic.ttf',
        '/usr/share/fonts/truetype/nanum/NanumBarunGothic.ttf',
    ])

    # macOS 시스템 폰트 경로
    font_paths.extend([
        '/Library/Fonts/AppleGothic.ttf',
        '/System/Library/Fonts/AppleSDGothicNeo.ttc',
    ])

    for font_path in font_paths:
        if os.path.exists(font_path):
            try:
                pdfmetrics.registerFont(TTFont('Korean', font_path))
                print(f'[INFO] Korean font registered: {font_path}')
                return 'Korean'
            except Exception as e:
                print(f'[WARN] Failed to register {font_path}: {e}')
                continue

    print('[ERROR] No Korean font found. Korean text may not display correctly.')
    return 'Helvetica'  # 폴백


def create_component_box(drawing, x, y, width, height, text, bg_color, text_color=colors.white, font_name='Korean'):
    """컴포넌트 박스 생성"""
    # 박스
    rect = Rect(x, y, width, height)
    rect.fillColor = bg_color
    rect.strokeColor = colors.black
    rect.strokeWidth = 2
    drawing.add(rect)

    # 텍스트
    lines = text.split('\n')
    y_offset = y + height - 15
    for line in lines:
        string = String(x + width/2, y_offset, line, textAnchor='middle', fontSize=10, fontName=font_name)
        string.fillColor = text_color
        drawing.add(string)
        y_offset -= 12


def create_arrow(drawing, x1, y1, x2, y2, label='', font_name='Korean'):
    """화살표 생성"""
    # 선
    line = Line(x1, y1, x2, y2)
    line.strokeColor = colors.HexColor('#2C3E50')
    line.strokeWidth = 2
    drawing.add(line)

    # 화살표 머리
    if x2 > x1:  # 오른쪽 화살표
        arrow_head = Polygon([x2, y2, x2-8, y2-4, x2-8, y2+4])
    elif x2 < x1:  # 왼쪽 화살표
        arrow_head = Polygon([x2, y2, x2+8, y2-4, x2+8, y2+4])
    elif y2 > y1:  # 위쪽 화살표
        arrow_head = Polygon([x2, y2, x2-4, y2-8, x2+4, y2-8])
    else:  # 아래쪽 화살표
        arrow_head = Polygon([x2, y2, x2-4, y2+8, x2+4, y2+8])

    arrow_head.fillColor = colors.HexColor('#2C3E50')
    arrow_head.strokeColor = colors.HexColor('#2C3E50')
    drawing.add(arrow_head)

    # 라벨
    if label:
        mid_x = (x1 + x2) / 2
        mid_y = (y1 + y2) / 2 + 5
        label_string = String(mid_x, mid_y, label, textAnchor='middle', fontSize=8, fontName=font_name)
        label_string.fillColor = colors.HexColor('#E74C3C')
        drawing.add(label_string)


def create_architecture_diagram():
    """아키텍처 다이어그램 생성"""
    drawing = Drawing(700, 420)

    # 색상 정의
    color_python = colors.HexColor('#3776AB')
    color_flink = colors.HexColor('#E6526F')
    color_spring = colors.HexColor('#6DB33F')
    color_react = colors.HexColor('#61DAFB')
    color_kafka = colors.HexColor('#231F20')

    # 레이어별 컴포넌트
    # Layer 1: Data Generation
    create_component_box(drawing, 20, 380, 140, 60,
                        'Transaction\nGenerator\n(Python)', color_python)

    # Layer 2: Message Broker
    create_component_box(drawing, 200, 420, 120, 40,
                        'Kafka Topic\nvirtual-transactions', color_kafka)
    create_component_box(drawing, 200, 340, 120, 40,
                        'Kafka Topic\ntransaction-alerts', color_kafka)

    # Layer 3: Stream Processing
    create_component_box(drawing, 360, 380, 140, 60,
                        'Fraud Detector\n(Apache Flink)\n(Scala/Java)', color_flink)

    # Layer 4: Backend Services
    create_component_box(drawing, 540, 420, 120, 40,
                        'Alert Service\n(Spring WebFlux)', color_spring)
    create_component_box(drawing, 540, 340, 120, 40,
                        'WebSocket\nGateway', color_spring)

    # Layer 5: Frontend
    create_component_box(drawing, 700, 380, 120, 60,
                        'Dashboard\n(React +\nTypeScript)', color_react)

    # Detection Rules (아래쪽)
    create_component_box(drawing, 360, 280, 140, 50,
                        'Detection Rules:\n• 고액 거래\n• 해외 거래\n• 빈번한 거래',
                        colors.HexColor('#95A5A6'), colors.black)

    # 화살표 및 데이터 흐름
    # Transaction Generator → Kafka
    create_arrow(drawing, 160, 410, 200, 440, 'publish')

    # Kafka → Fraud Detector
    create_arrow(drawing, 320, 440, 360, 410, 'consume')

    # Fraud Detector → Kafka Alerts
    create_arrow(drawing, 430, 380, 320, 360, 'publish')

    # Kafka Alerts → Alert Service
    create_arrow(drawing, 320, 360, 540, 440, 'consume')

    # Alert Service → WebSocket Gateway
    create_arrow(drawing, 600, 420, 600, 380, 'poll\n(1s)')

    # WebSocket Gateway → Dashboard
    create_arrow(drawing, 660, 360, 700, 410, 'push\n(WebSocket)')

    # Rules 연결
    create_arrow(drawing, 430, 330, 430, 380, '')

    # 범례
    legend_y = 220
    drawing.add(String(20, legend_y, '범례:', fontSize=12, fillColor=colors.black, fontName='Korean'))

    create_component_box(drawing, 20, legend_y - 30, 60, 20, 'Python', color_python)
    create_component_box(drawing, 90, legend_y - 30, 60, 20, 'Flink', color_flink)
    create_component_box(drawing, 160, legend_y - 30, 60, 20, 'Spring', color_spring)
    create_component_box(drawing, 230, legend_y - 30, 60, 20, 'React', color_react)
    create_component_box(drawing, 300, legend_y - 30, 60, 20, 'Kafka', color_kafka)

    # 성능 지표 박스
    perf_y = 80
    perf_box = Rect(20, perf_y - 40, 660, 70)
    perf_box.fillColor = colors.HexColor('#ECF0F1')
    perf_box.strokeColor = colors.HexColor('#34495E')
    perf_box.strokeWidth = 1
    drawing.add(perf_box)

    drawing.add(String(400, perf_y + 15, '성능 목표', textAnchor='middle',
                      fontSize=12, fillColor=colors.black, fontName='Korean'))
    drawing.add(String(400, perf_y, '• 종단 간 지연 시간: 평균 3초, 최대 5초',
                      textAnchor='middle', fontSize=9, fillColor=colors.black, fontName='Korean'))
    drawing.add(String(400, perf_y - 15, '• 처리량: 초당 10개 거래 (목표), 초당 100개 (최대)',
                      textAnchor='middle', fontSize=9, fillColor=colors.black, fontName='Korean'))
    drawing.add(String(400, perf_y - 30, '• 시스템 시작 시간: 5분 이내 | 가용성: 99%+',
                      textAnchor='middle', fontSize=9, fillColor=colors.black, fontName='Korean'))

    return drawing


def create_data_flow_diagram():
    """데이터 흐름 다이어그램 생성"""
    drawing = Drawing(700, 350)

    # 순차적 흐름 표현
    stages = [
        ('1. 거래 생성', colors.HexColor('#3498DB'), 50, 300),
        ('2. Kafka 발행', colors.HexColor('#2ECC71'), 200, 300),
        ('3. 규칙 적용', colors.HexColor('#E74C3C'), 350, 300),
        ('4. 알림 발행', colors.HexColor('#F39C12'), 500, 300),
        ('5. 저장/전송', colors.HexColor('#9B59B6'), 650, 300),
    ]

    for i, (label, color, x, y) in enumerate(stages):
        # 박스
        create_component_box(drawing, x, y, 120, 50, label, color)

        # 화살표 (마지막 제외)
        if i < len(stages) - 1:
            create_arrow(drawing, x + 120, y + 25, stages[i+1][2], y + 25, '')

    # 세부 정보
    details_y = 200
    detail_text = [
        'Transaction 생성 → virtual-transactions 토픽',
        'Flink 3가지 규칙 적용 (고액/해외/빈번)',
        'Alert 생성 → transaction-alerts 토픽',
        'Alert Service 저장 (최근 100개)',
        'WebSocket으로 브라우저 푸시 (1초 이내)'
    ]

    for i, text in enumerate(detail_text):
        drawing.add(String(400, details_y - (i * 20), f'• {text}',
                          textAnchor='middle', fontSize=9, fillColor=colors.black, fontName='Korean'))

    # 시간 라인
    timeline_y = 50
    timeline = Rect(50, timeline_y, 600, 30)
    timeline.fillColor = colors.HexColor('#E8F8F5')
    timeline.strokeColor = colors.HexColor('#1ABC9C')
    timeline.strokeWidth = 2
    drawing.add(timeline)

    drawing.add(String(400, timeline_y + 15,
                      '전체 흐름 시간: 거래 발생 → 화면 표시까지 평균 3초, 최대 5초',
                      textAnchor='middle', fontSize=10, fillColor=colors.black, fontName='Korean'))

    return drawing


def generate_pdf():
    """PDF 생성 메인 함수"""
    # 한글 폰트 등록
    korean_font = register_korean_font()

    filename = 'RealFDS_Architecture.pdf'
    doc = SimpleDocTemplate(
        filename,
        pagesize=landscape(A4),
        rightMargin=1.5*cm,
        leftMargin=1.5*cm,
        topMargin=2*cm,
        bottomMargin=2*cm
    )

    # 스타일 정의
    styles = getSampleStyleSheet()

    title_style = ParagraphStyle(
        'CustomTitle',
        parent=styles['Title'],
        fontSize=24,
        textColor=colors.HexColor('#2C3E50'),
        spaceAfter=30,
        alignment=TA_CENTER,
        fontName=korean_font
    )

    heading1_style = ParagraphStyle(
        'CustomHeading1',
        parent=styles['Heading1'],
        fontSize=16,
        textColor=colors.HexColor('#34495E'),
        spaceAfter=12,
        spaceBefore=12,
        fontName=korean_font
    )

    heading2_style = ParagraphStyle(
        'CustomHeading2',
        parent=styles['Heading2'],
        fontSize=14,
        textColor=colors.HexColor('#7F8C8D'),
        spaceAfter=10,
        fontName=korean_font
    )

    body_style = ParagraphStyle(
        'CustomBody',
        parent=styles['Normal'],
        fontSize=10,
        alignment=TA_JUSTIFY,
        spaceAfter=8,
        fontName=korean_font
    )

    # 문서 구성
    story = []

    # === 제목 페이지 ===
    story.append(Spacer(1, 2*cm))
    story.append(Paragraph('실시간 금융 거래 탐지 시스템', title_style))
    story.append(Paragraph('RealFDS Architecture', title_style))
    story.append(Spacer(1, 1*cm))

    # 프로젝트 정보 테이블
    project_info = [
        ['프로젝트명', 'RealFDS (Real-time Financial Detection System)'],
        ['버전', '1.0'],
        ['작성일', datetime.now().strftime('%Y년 %m월 %d일')],
        ['아키텍처', '이벤트 기반 마이크로서비스 (5개 서비스)'],
        ['목적', '실시간 금융 거래 모니터링 및 사기 패턴 탐지']
    ]

    project_table = Table(project_info, colWidths=[5*cm, 15*cm])
    project_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (0, -1), colors.HexColor('#34495E')),
        ('TEXTCOLOR', (0, 0), (0, -1), colors.white),
        ('BACKGROUND', (1, 0), (1, -1), colors.HexColor('#ECF0F1')),
        ('TEXTCOLOR', (1, 0), (1, -1), colors.black),
        ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
        ('FONTNAME', (0, 0), (-1, -1), korean_font),
        ('FONTSIZE', (0, 0), (-1, -1), 10),
        ('GRID', (0, 0), (-1, -1), 1, colors.grey),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('LEFTPADDING', (0, 0), (-1, -1), 10),
        ('RIGHTPADDING', (0, 0), (-1, -1), 10),
        ('TOPPADDING', (0, 0), (-1, -1), 8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
    ]))

    story.append(project_table)
    story.append(PageBreak())

    # === 시스템 개요 ===
    story.append(Paragraph('1. 시스템 개요', heading1_style))
    story.append(Paragraph(
        'RealFDS는 Apache Kafka를 메시지 브로커로, Apache Flink를 스트림 처리 엔진으로 사용하여 '
        '금융 거래를 실시간으로 모니터링하고 의심스러운 패턴을 즉시 탐지하는 마이크로서비스 '
        '아키텍처 기반 시스템입니다. 거래 발생부터 알림 표시까지 5초 이내의 종단 간 지연 시간을 '
        '목표로 하며, Docker Compose를 사용하여 단일 명령어로 전체 시스템을 실행할 수 있습니다.',
        body_style
    ))
    story.append(Spacer(1, 0.5*cm))

    # 주요 기능
    story.append(Paragraph('1.1 주요 기능', heading2_style))
    features = [
        '• <b>고액 거래 탐지</b>: 100만원 초과 거래 자동 탐지',
        '• <b>해외 거래 탐지</b>: 한국 외 지역 거래 실시간 모니터링',
        '• <b>빈번한 거래 탐지</b>: 1분 내 5회 초과 거래 패턴 분석',
        '• <b>실시간 알림</b>: WebSocket을 통한 브라우저 실시간 푸시',
        '• <b>간편한 실행</b>: docker-compose up 단일 명령어로 전체 시스템 구동'
    ]

    for feature in features:
        story.append(Paragraph(feature, body_style))

    story.append(Spacer(1, 0.5*cm))

    # 기술 스택
    story.append(Paragraph('1.2 기술 스택', heading2_style))

    tech_stack = [
        ['구분', '기술', '용도'],
        ['메시지 브로커', 'Apache Kafka 3.6+', '서비스 간 이벤트 스트리밍'],
        ['스트림 처리', 'Apache Flink 1.18+ (Scala)', '실시간 거래 탐지 엔진'],
        ['백엔드', 'Spring Boot 3.2+ WebFlux (Java 17)', '알림 서비스 및 WebSocket 게이트웨이'],
        ['프론트엔드', 'React 18 + TypeScript 5 + Vite', '실시간 대시보드'],
        ['데이터 생성', 'Python 3.11+', '가상 거래 데이터 생성기'],
        ['컨테이너', 'Docker + Docker Compose', '전체 시스템 오케스트레이션'],
        ['상태 관리', 'Flink RocksDB State Backend', '빈번한 거래 탐지용 상태 저장']
    ]

    tech_table = Table(tech_stack, colWidths=[4*cm, 7*cm, 9*cm])
    tech_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#2C3E50')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
        ('FONTNAME', (0, 0), (-1, -1), korean_font),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
        ('BACKGROUND', (0, 1), (-1, -1), colors.beige),
        ('GRID', (0, 0), (-1, -1), 1, colors.grey),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('LEFTPADDING', (0, 0), (-1, -1), 8),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
    ]))

    story.append(tech_table)
    story.append(PageBreak())

    # === 아키텍처 다이어그램 ===
    story.append(Paragraph('2. 시스템 아키텍처', heading1_style))
    story.append(Spacer(1, 0.3*cm))

    arch_diagram = create_architecture_diagram()
    story.append(arch_diagram)

    story.append(Spacer(1, 0.5*cm))
    story.append(PageBreak())

    # === 컴포넌트 설명 ===
    story.append(Paragraph('3. 컴포넌트 상세 설명', heading1_style))

    components = [
        {
            'name': '3.1 Transaction Generator (거래 생성기)',
            'tech': 'Python 3.11+',
            'desc': '테스트 및 데모를 위한 가상 거래 데이터를 주기적으로 생성합니다. '
                   '10명의 가상 사용자 풀에서 무작위로 선택하여 1,000원~1,500,000원 사이의 '
                   '거래를 생성하고, Kafka virtual-transactions 토픽으로 발행합니다.',
            'features': [
                '• 거래 금액, 국가 코드, 사용자 ID 무작위 생성',
                '• 데모 효과를 위해 고액/해외 거래 포함',
                '• Kafka Producer로 실시간 스트리밍'
            ]
        },
        {
            'name': '3.2 Fraud Detector (탐지 엔진)',
            'tech': 'Apache Flink 1.18+ (Scala/Java)',
            'desc': 'Kafka에서 거래 스트림을 소비하고 3가지 탐지 규칙을 적용하여 의심스러운 '
                   '패턴을 실시간으로 탐지합니다. 상태 기반(stateful) 처리를 통해 빈번한 거래 '
                   '패턴을 추적합니다.',
            'features': [
                '• HIGH_VALUE: 100만원 초과 거래 탐지',
                '• FOREIGN_COUNTRY: KR 외 국가 거래 탐지',
                '• HIGH_FREQUENCY: 1분 내 5회 초과 거래 탐지 (상태 관리)',
                '• RocksDB State Backend로 사용자별 상태 유지',
                '• Event-time 처리 및 Watermark 사용'
            ]
        },
        {
            'name': '3.3 Alert Service (알림 서비스)',
            'tech': 'Spring Boot 3.2 WebFlux (Java 17)',
            'desc': 'Kafka transaction-alerts 토픽에서 알림을 소비하여 인메모리 저장소에 보관합니다. '
                   '최근 100개의 알림만 유지하며, REST API를 통해 조회를 제공합니다.',
            'features': [
                '• Reactive Kafka Consumer (Spring Kafka)',
                '• ConcurrentDeque를 사용한 인메모리 저장소',
                '• REST API (/api/alerts) 제공',
                '• Health check 엔드포인트'
            ]
        },
        {
            'name': '3.4 WebSocket Gateway (웹소켓 게이트웨이)',
            'tech': 'Spring Boot 3.2 WebFlux (Java 17)',
            'desc': 'Alert Service를 1초마다 폴링하여 새로운 알림을 감지하고, 연결된 모든 '
                   'WebSocket 클라이언트에게 실시간으로 브로드캐스트합니다.',
            'features': [
                '• WebSocket 연결 관리 (Spring WebFlux WebSocket)',
                '• Alert Service REST API 폴링 (1초 주기)',
                '• 다중 클라이언트 브로드캐스트',
                '• 자동 재연결 지원'
            ]
        },
        {
            'name': '3.5 Frontend Dashboard (프론트엔드 대시보드)',
            'tech': 'React 18 + TypeScript 5 + Vite',
            'desc': '보안 담당자가 웹 브라우저를 통해 실시간 알림을 모니터링할 수 있는 대시보드입니다. '
                   'WebSocket을 통해 알림을 실시간으로 수신하고 화면에 표시합니다.',
            'features': [
                '• WebSocket 클라이언트 (useWebSocket hook)',
                '• 실시간 알림 목록 표시 (최근 100개)',
                '• 연결 상태 표시 (connected/disconnected)',
                '• 자동 재연결 (5초 간격)',
                '• 반응형 UI'
            ]
        }
    ]

    for comp in components:
        story.append(Paragraph(comp['name'], heading2_style))
        story.append(Paragraph(f"<b>기술:</b> {comp['tech']}", body_style))
        story.append(Paragraph(comp['desc'], body_style))
        for feature in comp['features']:
            story.append(Paragraph(feature, body_style))
        story.append(Spacer(1, 0.3*cm))

    story.append(PageBreak())

    # === 데이터 흐름 ===
    story.append(Paragraph('4. 데이터 흐름', heading1_style))
    story.append(Spacer(1, 0.3*cm))

    flow_diagram = create_data_flow_diagram()
    story.append(flow_diagram)

    story.append(Spacer(1, 0.5*cm))

    # 상세 흐름
    story.append(Paragraph('4.1 정상 흐름 시나리오', heading2_style))
    flow_steps = [
        '<b>Step 1</b>: Transaction Generator가 가상 거래 생성 (거래 ID, 사용자 ID, 금액, 국가 등)',
        '<b>Step 2</b>: Kafka Producer가 virtual-transactions 토픽에 발행 (userId를 키로 파티셔닝)',
        '<b>Step 3</b>: Fraud Detector (Flink)가 거래 소비 및 3가지 규칙 적용',
        '<b>Step 4</b>: 조건 충족 시 Alert 생성 → transaction-alerts 토픽에 발행',
        '<b>Step 5</b>: Alert Service가 알림 소비 및 인메모리 저장소에 추가 (최근 100개)',
        '<b>Step 6</b>: WebSocket Gateway가 1초마다 Alert Service 폴링',
        '<b>Step 7</b>: 새 알림 감지 시 연결된 모든 WebSocket 클라이언트에 브로드캐스트',
        '<b>Step 8</b>: Frontend Dashboard가 알림 수신 및 React State 업데이트 → UI 렌더링'
    ]

    for step in flow_steps:
        story.append(Paragraph(step, body_style))

    story.append(Spacer(1, 0.5*cm))

    # Kafka 토픽 정보
    story.append(Paragraph('4.2 Kafka 토픽 구성', heading2_style))

    kafka_topics = [
        ['토픽명', '용도', '파티션', '보관 기간', '키'],
        ['virtual-transactions', '거래 스트림', '3개', '1시간', 'userId'],
        ['transaction-alerts', '알림 스트림', '3개', '24시간', 'userId'],
    ]

    kafka_table = Table(kafka_topics, colWidths=[5*cm, 6*cm, 3*cm, 3*cm, 3*cm])
    kafka_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#34495E')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('FONTNAME', (0, 0), (-1, -1), korean_font),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
        ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#ECF0F1')),
        ('GRID', (0, 0), (-1, -1), 1, colors.grey),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
    ]))

    story.append(kafka_table)
    story.append(PageBreak())

    # === 탐지 규칙 ===
    story.append(Paragraph('5. 탐지 규칙 상세', heading1_style))

    detection_rules = [
        ['규칙명', '유형', '조건', '심각도', '사유 예시'],
        ['HIGH_VALUE\n(고액 거래)', 'SIMPLE_RULE', 'amount > 1,000,000', 'HIGH',
         '고액 거래 (100만원 초과):\n1,250,000원'],
        ['FOREIGN_COUNTRY\n(해외 거래)', 'SIMPLE_RULE', 'countryCode != "KR"', 'MEDIUM',
         '해외 거래 탐지\n(국가: US)'],
        ['HIGH_FREQUENCY\n(빈번한 거래)', 'STATEFUL_RULE', '1분 내 5회 초과', 'HIGH',
         '빈번한 거래 (1분 내 5회 초과):\nuser-3, 6회'],
    ]

    rules_table = Table(detection_rules, colWidths=[4*cm, 3.5*cm, 4*cm, 2.5*cm, 6*cm])
    rules_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#E74C3C')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('FONTNAME', (0, 0), (-1, -1), korean_font),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
        ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#FADBD8')),
        ('GRID', (0, 0), (-1, -1), 1, colors.grey),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('TOPPADDING', (0, 0), (-1, -1), 8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
    ]))

    story.append(rules_table)
    story.append(Spacer(1, 0.5*cm))

    story.append(Paragraph(
        '<b>참고</b>: STATEFUL_RULE인 HIGH_FREQUENCY는 Flink의 RocksDB State Backend를 '
        '사용하여 사용자별 거래 이력을 1분 윈도우 내에서 추적합니다. Event-time 처리와 Watermark를 '
        '사용하여 정확한 시간 기반 집계를 수행합니다.',
        body_style
    ))

    story.append(Spacer(1, 0.5*cm))

    # === 성능 및 목표 ===
    story.append(Paragraph('6. 성능 목표 및 제약사항', heading1_style))

    perf_data = [
        ['항목', '목표', '허용 최대', '측정 방법'],
        ['종단 간 지연 시간', '평균 3초', 'p95 5초, 최대 8초',
         '타임스탬프 차이 측정'],
        ['처리량', '초당 10개 거래', '초당 100개',
         'Kafka 메트릭'],
        ['알림 표시 지연', '1초 이내', '2초',
         'WebSocket 수신 → UI 렌더링'],
        ['시스템 시작 시간', '5분 이내', '-',
         'docker-compose up 완료'],
        ['총 메모리 사용량', '<4GB', '-',
         '모든 컨테이너 합계'],
        ['시스템 가용성', '99%', '30분 무중단',
         '안정성 테스트'],
    ]

    perf_table = Table(perf_data, colWidths=[5*cm, 4*cm, 5*cm, 6*cm])
    perf_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#27AE60')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('FONTNAME', (0, 0), (-1, -1), korean_font),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
        ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#D5F4E6')),
        ('GRID', (0, 0), (-1, -1), 1, colors.grey),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('TOPPADDING', (0, 0), (-1, -1), 8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
    ]))

    story.append(perf_table)
    story.append(Spacer(1, 0.5*cm))

    # 제약사항
    story.append(Paragraph('6.1 제약사항', heading2_style))
    constraints = [
        '• <b>로컬 실행 전용</b>: 클라우드 서비스 미사용, Docker Compose로만 실행',
        '• <b>인메모리 저장소</b>: 시스템 재시작 시 알림 이력 초기화',
        '• <b>단일 브로커</b>: Kafka 복제 계수 1 (고가용성 미지원)',
        '• <b>최소 사양</b>: 8GB RAM, 4 core CPU 권장',
        '• <b>가상 데이터</b>: 실제 금융 시스템 연동 없음 (데모/학습 목적)'
    ]

    for constraint in constraints:
        story.append(Paragraph(constraint, body_style))

    story.append(PageBreak())

    # === 실행 방법 ===
    story.append(Paragraph('7. 시스템 실행 방법', heading1_style))

    story.append(Paragraph('7.1 사전 요구사항', heading2_style))
    prereqs = [
        '• Docker 20.10+ 설치',
        '• Docker Compose 2.0+ 설치',
        '• 최소 8GB RAM, 4 core CPU',
        '• 포트 가용성: 9092 (Kafka), 8081 (Alert Service), 8082 (WebSocket), 8083 (Frontend)'
    ]
    for prereq in prereqs:
        story.append(Paragraph(prereq, body_style))

    story.append(Spacer(1, 0.3*cm))
    story.append(Paragraph('7.2 실행 명령어', heading2_style))

    # 코드 스타일
    code_style = ParagraphStyle(
        'Code',
        parent=styles['Code'],
        fontSize=9,
        leftIndent=20,
        spaceAfter=6,
        fontName='Courier'
    )

    story.append(Paragraph('# 1. 프로젝트 디렉토리로 이동', code_style))
    story.append(Paragraph('cd RealFDS', code_style))
    story.append(Spacer(1, 0.2*cm))

    story.append(Paragraph('# 2. 전체 시스템 실행', code_style))
    story.append(Paragraph('docker-compose up', code_style))
    story.append(Spacer(1, 0.2*cm))

    story.append(Paragraph('# 3. 웹 브라우저에서 대시보드 접속', code_style))
    story.append(Paragraph('http://localhost:8083', code_style))
    story.append(Spacer(1, 0.2*cm))

    story.append(Paragraph('# 4. 시스템 종료', code_style))
    story.append(Paragraph('docker-compose down', code_style))

    story.append(Spacer(1, 0.5*cm))

    story.append(Paragraph(
        '<b>참고</b>: 시스템 시작 후 5분 이내에 모든 서비스가 준비되며, '
        '대시보드에 접속하면 자동으로 실시간 알림 스트리밍이 시작됩니다.',
        body_style
    ))

    story.append(PageBreak())

    # === 결론 ===
    story.append(Paragraph('8. 결론 및 향후 계획', heading1_style))

    story.append(Paragraph(
        'RealFDS는 이벤트 기반 마이크로서비스 아키텍처의 핵심 개념을 실습할 수 있는 '
        '학습용 프로젝트입니다. Apache Kafka와 Apache Flink를 활용한 실시간 스트림 처리, '
        'WebSocket을 통한 양방향 통신, Docker를 사용한 컨테이너 오케스트레이션 등 '
        '현대적인 분산 시스템 기술을 경험할 수 있습니다.',
        body_style
    ))

    story.append(Spacer(1, 0.3*cm))
    story.append(Paragraph('8.1 향후 개선 사항', heading2_style))

    future_items = [
        '• <b>영속성 추가</b>: PostgreSQL 또는 MongoDB를 사용한 알림 이력 저장',
        '• <b>머신러닝 탐지</b>: 과거 데이터 학습을 통한 이상 패턴 자동 탐지',
        '• <b>동적 규칙 관리</b>: 시스템 재시작 없이 웹 UI에서 새로운 탐지 규칙 추가',
        '• <b>사용자 인증</b>: 여러 보안 담당자의 개별 계정 및 역할 기반 접근 제어',
        '• <b>알림 통계</b>: 시간대별, 규칙별 알림 발생 추이 시각화',
        '• <b>클라우드 배포</b>: Kubernetes를 사용한 운영 환경 배포',
        '• <b>모바일 지원</b>: 반응형 UI 또는 모바일 앱 개발'
    ]

    for item in future_items:
        story.append(Paragraph(item, body_style))

    story.append(Spacer(1, 0.5*cm))

    # 푸터 정보
    footer_style = ParagraphStyle(
        'Footer',
        parent=styles['Normal'],
        fontSize=8,
        textColor=colors.grey,
        alignment=TA_CENTER,
        fontName=korean_font
    )

    story.append(Spacer(1, 1*cm))
    story.append(Paragraph('───────────────────────────────────────', footer_style))
    story.append(Paragraph(
        f'문서 생성일: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")} | '
        'RealFDS v1.0 Architecture Document',
        footer_style
    ))

    # PDF 빌드
    doc.build(story)
    print(f'[SUCCESS] PDF created: {filename}')
    print(f'[INFO] File size: {os.path.getsize(filename) / 1024:.1f} KB')
    print(f'[INFO] Location: {os.path.abspath(filename)}')


if __name__ == '__main__':
    generate_pdf()
