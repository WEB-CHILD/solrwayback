<template>
  <div class="domainVisitContainer">
    <h2 class="toolboxHeadline">
      Visited sites on domain
    </h2>
    <p>Enter a domain to visualise how many websites from that domain you have looked at during your current session.</p>
    <p>The URLs are laid out as circles on the canvas below following a chronological order of harvest.</p>
    <div class="contentContainer">
      <div class="inputContainer">
        <input 
          v-model="domain"
          type="text"
          placeholder="Enter domain (e.g., example.com)"
          class="domainInput"
          @keyup.enter="showVisualization"
        >
        <button 
          class="generateButton"
          :disabled="!domain.trim()"
          @click="showVisualization"
        >
          {{ rendered ? 'Update' : 'Visualise' }}
        </button>
      </div>
      
      <div v-if="showCanvas && domainUrls.length > 0" class="infoBox">
        <span><strong>Total URLs:</strong> {{ domainUrls.length }}</span>
        <span class="separator">|</span>
        <span><strong>Visited:</strong> {{ visitedCount }} ({{ visitedPercentage }}%)</span>
      </div>
      
      <div v-if="showCanvas" class="canvasContainer">
        <canvas 
          ref="visualizationCanvas"
          @mousemove="handleMouseMove"
          @mouseleave="hideTooltip"
          @click="handleCircleClick"
        />
        <div 
          v-if="tooltip.visible" 
          class="tooltip"
          :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }"
        >
          {{ tooltip.text }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { requestService } from '../../services/RequestService'
import configs from '../../configs';


export default {
  name: 'DomainVisitVisualization',
  data() {
    return {
      domain: '',
      showCanvas: false,
      circleRadius: 20,
        rendered: false,
      domainUrls: [],
      navigationHistory: [],
      circlePositions: [],
      tooltip: {
        visible: false,
        x: 0,
        y: 0,
        text: ''
      }
    }
  },
  computed: {
    visitedCount() {
      return this.domainUrls.filter(url => this.navigationHistory.includes(url)).length
    },
    visitedPercentage() {
      if (this.domainUrls.length === 0) return 0
      return ((this.visitedCount / this.domainUrls.length) * 100).toFixed(1)
    }
  },
  methods: {
    /**
     * Show the visualization canvas and trigger rendering.
     * Marks `showCanvas` true and schedules `drawCircles` on next tick.
     * @returns {void}
     */
    showVisualization() {
      this.showCanvas = true

      // Wait for next tick to ensure canvas is rendered
      this.$nextTick(() => {
        this.drawCircles()
      })
    },
    
    /**
     * Fetch domain URLs and navigation history, calculate a grid layout
     * and draw the circles on the canvas. Stores circle positions in
     * `circlePositions` for hover and click interactions.
     *
     * This method is async because it performs network requests via
     * `requestService`.
     * @returns {Promise<void>}
     */
    async drawCircles() {
      const canvas = this.$refs.visualizationCanvas
      if (!canvas) return
      
      const ctx = canvas.getContext('2d')
      const container = canvas.parentElement

      // Fetch domain URLs from backend
      try {
        this.domainUrls = await requestService.getDomainUrls(this.domain.trim())
      } catch (error) {
        console.error('Error fetching domain URLs:', error)
        return
      }

      try {
        const fullHistory = await requestService.getNavigationHistory()
        this.navigationHistory = fullHistory
          .filter(entry => entry.archivedUrl)
          .map(entry => {
            const url = entry.archivedUrl
            const webIndex = url.indexOf('web/')
            return webIndex !== -1 ? url.substring(webIndex + 4) : url
          })
      } catch (error) {
        console.error('Error fetching domain URLs:', error)
        return
      }

      // Calculate grid layout
      const totalCircles = this.domainUrls.length
      const padding = 30
      const circleDiameter = this.circleRadius * 2
      const spacing = 10
      const cellSize = circleDiameter + spacing
      
      // Calculate circles per row based on canvas width
      const circlesPerRow = Math.floor((container.clientWidth - padding * 2) / cellSize)
      const numRows = Math.ceil(totalCircles / circlesPerRow)
      
      // Set canvas size based on grid
      canvas.width = container.clientWidth
      canvas.height = numRows * cellSize + padding * 2
      
      // Clear canvas
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      
      // Reset circle positions
      this.circlePositions = []
      
      // Draw circles in grid layout
      for (let i = 0; i < totalCircles; i++) {
        const row = Math.floor(i / circlesPerRow)
        const col = i % circlesPerRow
        
        const x = padding + col * cellSize + this.circleRadius
        const y = padding + row * cellSize + this.circleRadius
        
        // Store circle position and URL for hover detection
        this.circlePositions.push({
          x,
          y,
          url: this.domainUrls[i]
        })
        
        

        if (this.navigationHistory.includes(this.domainUrls[i])){
          const greeHex = "#4CAF50"
          ctx.beginPath()
          ctx.arc(x, y, this.circleRadius, 0, 2 * Math.PI)
          ctx.fillStyle = greeHex 
          ctx.fill()
          ctx.strokeStyle = greeHex
          ctx.lineWidth = 2
          ctx.stroke()
        } else {
          const greyHex = "#b3b3b3"
          ctx.beginPath()
          ctx.arc(x, y, this.circleRadius, 0, 2 * Math.PI)
          ctx.fillStyle = greyHex 
          ctx.fill()
          ctx.strokeStyle = greyHex
          ctx.lineWidth = 2
          ctx.stroke()
        }
      }
      
      // Mark that a visualization has been rendered
      this.rendered = true
      
    },
    
    /**
     * Handle mouse movement over the canvas and show a tooltip for the
     * circle currently under the pointer (if any).
     * @param {MouseEvent} event - Mouse move event from the canvas.
     * @returns {void}
     */
    handleMouseMove(event) {
      const canvas = this.$refs.visualizationCanvas
      const rect = canvas.getBoundingClientRect()
      const mouseX = event.clientX - rect.left
      const mouseY = event.clientY - rect.top
      
      // Find if mouse is over a circle
      const hoveredCircle = this.circlePositions.find(circle => {
        const distance = Math.sqrt(
          Math.pow(mouseX - circle.x, 2) + 
          Math.pow(mouseY - circle.y, 2)
        )
        return distance <= this.circleRadius
      })
      
      if (hoveredCircle) {
        this.tooltip.visible = true
        this.tooltip.x = event.clientX + 10
        this.tooltip.y = event.clientY + 10
        this.tooltip.text = hoveredCircle.url
      } else {
        this.tooltip.visible = false
      }
    },
    
    /**
     * Hide the tooltip immediately.
     * @returns {void}
     */
    hideTooltip() {
      this.tooltip.visible = false
    },
    
    /**
     * Handle a click on the canvas. Determines which circle (if any) was
     * clicked and opens a playback window for that resource.
     *
     * @param {MouseEvent} event - Click event from the canvas.
     * @returns {void}
     */
    handleCircleClick(event) {
      const canvas = this.$refs.visualizationCanvas
      const rect = canvas.getBoundingClientRect()
      const mouseX = event.clientX - rect.left
      const mouseY = event.clientY - rect.top
      
      // Find which circle was clicked
      const clickedCircle = this.circlePositions.find(circle => {
        const distance = Math.sqrt(
          Math.pow(mouseX - circle.x, 2) + 
          Math.pow(mouseY - circle.y, 2)
        )
        return distance <= this.circleRadius
      })
      
      if (clickedCircle) {
                
        const solrWaybackUrl = configs.playbackConfig.solrwaybackBaseURL 
        let playbackUrl = `${solrWaybackUrl}services/web/${clickedCircle.url}`
      
        window.open(playbackUrl, '_blank')
      }
    }
  }
}
</script>

<style scoped>
.domainVisitContainer {
  padding: 20px;
}

.toolboxHeadline {
  margin-bottom: 20px;
}

.contentContainer {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.inputContainer {
  display: flex;
  gap: 10px;
  align-items: center;
}

.domainInput {
  flex: 1;
  padding: 12px 16px;
  font-size: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 4px;
  transition: border-color 0.3s;
}

.domainInput:focus {
  outline: none;
  border-color: #4CAF50;
}

.infoBox {
  padding: 12px 16px;
  background-color: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 14px;
}

.infoBox .separator {
  color: #999;
}

.generateButton {
  padding: 12px 24px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: background-color 0.3s;
}

.generateButton:hover:not(:disabled) {
  background-color: #45a049;
}

.generateButton:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.canvasContainer {
  position: relative;
  width: 100%;
  background-color: #fafafa;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.canvasContainer canvas {
  display: block;
  cursor: pointer;
}

.tooltip {
  position: fixed;
  background-color: rgba(0, 0, 0, 0.85);
  color: white;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  pointer-events: none;
  z-index: 1000;
  max-width: 400px;
  word-break: break-all;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
</style>
