/**
 * 生成 tabbar PNG 图标
 * 直接用 Canvas API 绘制并保存为 PNG
 */

const fs = require('fs')
const path = require('path')

// 检查是否有 canvas 模块，没有的话用纯方式生成
try {
  const { createCanvas } = require('canvas')
  generateWithCanvas(createCanvas)
} catch (e) {
  console.log('未安装 canvas 模块，尝试使用替代方案...')
  generateWithPngjs()
}

function generateWithCanvas(createCanvas) {
  const outputDir = path.join(__dirname, 'src/static/tabbar')
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true })
  }

  const size = 128
  const strokeColor = '#999999'
  const activeColor = '#6c4ee6'

  // 绘制图标函数
  function drawIcon(name, drawFn, isActive) {
    const canvas = createCanvas(size, size)
    const ctx = canvas.getContext('2d')
    
    // 透明背景
    ctx.clearRect(0, 0, size, size)
    
    // 设置样式
    ctx.strokeStyle = isActive ? activeColor : strokeColor
    ctx.fillStyle = isActive ? activeColor : 'transparent'
    ctx.lineWidth = 4
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    
    drawFn(ctx, size, isActive)
    
    const fileName = isActive ? `${name}-active.png` : `${name}.png`
    const buffer = canvas.toBuffer('image/png')
    fs.writeFileSync(path.join(outputDir, fileName), buffer)
    console.log(`✅ 生成: ${fileName}`)
  }

  // 1. 首页 - 房子
  function drawHome(ctx, s, active) {
    const cx = s / 2
    const top = s * 0.2
    const bottom = s * 0.85
    const left = s * 0.2
    const right = s * 0.8
    const doorW = s * 0.18
    const doorH = s * 0.28

    ctx.beginPath()
    ctx.moveTo(left, s * 0.45)
    ctx.lineTo(cx, top)
    ctx.lineTo(right, s * 0.45)
    ctx.lineTo(right, bottom)
    ctx.lineTo(cx + doorW/2 + s*0.08, bottom)
    ctx.lineTo(cx + doorW/2 + s*0.08, s * 0.55)
    ctx.lineTo(cx - doorW/2 - s*0.08, s * 0.55)
    ctx.lineTo(cx - doorW/2 - s*0.08, bottom)
    ctx.lineTo(left, bottom)
    ctx.closePath()
    
    if (active) {
      ctx.fill()
    }
    ctx.stroke()
  }

  // 2. 校园频道 - 星球/轨道
  function drawChannel(ctx, s, active) {
    const cx = s / 2
    const cy = s / 2
    const r = s * 0.28

    // 星球
    ctx.beginPath()
    ctx.arc(cx, cy, r, 0, Math.PI * 2)
    if (active) {
      ctx.fillStyle = active + '30'
      ctx.fill()
      ctx.fillStyle = activeColor
    }
    ctx.stroke()

    // 轨道椭圆
    ctx.beginPath()
    ctx.ellipse(cx, cy, s * 0.42, s * 0.14, 0, 0, Math.PI * 2)
    ctx.setLineDash([6, 4])
    ctx.stroke()
    ctx.setLineDash([])

    // 中间的线
    ctx.beginPath()
    ctx.moveTo(cx, cy - r)
    ctx.lineTo(cx, cy + r)
    ctx.stroke()
  }

  // 3. 消息 - 对话气泡
  function drawMessage(ctx, s, active) {
    const left = s * 0.15
    const right = s * 0.85
    const top = s * 0.2
    const bottom = s * 0.75
    const r = 8
    const tailW = s * 0.15
    const tailH = s * 0.15

    ctx.beginPath()
    // 左上圆角
    ctx.moveTo(left + r, top)
    // 上边
    ctx.lineTo(right - r, top)
    // 右上圆角
    ctx.quadraticCurveTo(right, top, right, top + r)
    // 右边
    ctx.lineTo(right, bottom - r)
    // 右下圆角
    ctx.quadraticCurveTo(right, bottom, right - r, bottom)
    // 底边（到尾巴）
    ctx.lineTo(left + tailW + s*0.15, bottom)
    // 尾巴尖
    ctx.lineTo(left + tailW, bottom + tailH)
    ctx.lineTo(left + tailW, bottom)
    // 底边（左边）
    ctx.lineTo(left + r, bottom)
    // 左下圆角
    ctx.quadraticCurveTo(left, bottom, left, bottom - r)
    // 左边
    ctx.lineTo(left, top + r)
    // 左上圆角
    ctx.quadraticCurveTo(left, top, left + r, top)
    ctx.closePath()

    if (active) {
      ctx.fill()
    }
    ctx.stroke()
  }

  // 4. 我的 - 人形
  function drawMine(ctx, s, active) {
    const cx = s / 2
    const headY = s * 0.32
    const headR = s * 0.18
    const bodyTop = s * 0.55
    const bodyBottom = s * 0.85

    // 头
    ctx.beginPath()
    ctx.arc(cx, headY, headR, 0, Math.PI * 2)
    if (active) {
      ctx.fill()
    }
    ctx.stroke()

    // 身体（U型）
    ctx.beginPath()
    const bodyLeft = s * 0.22
    const bodyRight = s * 0.78
    ctx.moveTo(bodyLeft, bodyBottom)
    ctx.quadraticCurveTo(bodyLeft, bodyTop, cx, bodyTop)
    ctx.quadraticCurveTo(bodyRight, bodyTop, bodyRight, bodyBottom)
    if (active) {
      ctx.fill()
    }
    ctx.stroke()
  }

  // 生成所有图标
  console.log('')
  console.log('🎨 正在生成 tabbar 图标...')
  console.log('')

  drawIcon('home', drawHome, false)
  drawIcon('home', drawHome, true)
  drawIcon('channel', drawChannel, false)
  drawIcon('channel', drawChannel, true)
  drawIcon('message', drawMessage, false)
  drawIcon('message', drawMessage, true)
  drawIcon('mine', drawMine, false)
  drawIcon('mine', drawMine, true)

  console.log('')
  console.log('🎉 所有图标生成完成！')
  console.log(`📁 图标位置: ${outputDir}`)
  console.log('')
}

function generateWithPngjs() {
  console.log('')
  console.log('💡 提示：需要安装 canvas 模块才能生成 PNG 图标')
  console.log('')
  console.log('执行以下命令安装：')
  console.log('  npm install canvas --save-dev')
  console.log('')
  console.log('然后运行：')
  console.log('  node generate-icons.js')
  console.log('')
  console.log('或者你也可以直接去阿里图标库下载：')
  console.log('  https://www.iconfont.cn')
  console.log('  搜索：首页、星球、消息、用户')
  console.log('  下载 PNG 格式，放到 src/static/tabbar/ 目录')
  console.log('')
}
