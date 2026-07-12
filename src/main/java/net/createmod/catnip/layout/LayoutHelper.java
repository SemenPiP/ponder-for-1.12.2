package net.createmod.catnip.layout;

public interface LayoutHelper {
    static LayoutHelper centeredHorizontal(int itemCount, int rows, int width, int height, int spacing) {
        return new CenteredHorizontalLayoutHelper(itemCount, rows, width, height, spacing);
    }
    int getX();
    int getY();
    void next();
    int getTotalWidth();
    int getTotalHeight();
    default Area getArea() { return new Area(-getTotalWidth() / 2, -getTotalHeight() / 2, getTotalWidth(), getTotalHeight()); }

    final class Area {
        public final int x, y, width, height;
        public Area(int x, int y, int width, int height) { this.x=x;this.y=y;this.width=width;this.height=height; }
        public boolean contains(int px, int py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }

    final class CenteredHorizontalLayoutHelper implements LayoutHelper {
        private final int rows, width, height, spacing;
        private final int[] rowCounts;
        private int currentColumn, currentRow, x, y;
        CenteredHorizontalLayoutHelper(int itemCount, int rows, int width, int height, int spacing) {
            if (itemCount < 0 || rows <= 0 || width < 0 || height < 0) throw new IllegalArgumentException("Invalid layout dimensions");
            this.rows = Math.min(rows, Math.max(1, itemCount)); this.width=width;this.height=height;this.spacing=spacing;
            rowCounts = new int[this.rows];
            int perRow = itemCount / this.rows, extra = itemCount % this.rows;
            for (int i=0;i<this.rows;i++) rowCounts[i]=perRow+(i<extra?1:0);
            prepareX(); y=-getTotalHeight()/2;
        }
        public int getX(){return x;} public int getY(){return y;}
        public void next(){
            if (rowCounts[currentRow] == 0) return;
            currentColumn++;
            if(currentColumn>=rowCounts[currentRow]){if(++currentRow>=rows){x=0;y=0;return;}currentColumn=0;prepareX();y+=height+spacing;}
            else x+=width+spacing;
        }
        private void prepareX(){int rowWidth=rowCounts[currentRow]*width+Math.max(0,rowCounts[currentRow]-1)*spacing;x=-rowWidth/2;}
        public int getTotalWidth(){int max=0;for(int count:rowCounts)max=Math.max(max,count*width+Math.max(0,count-1)*spacing);return max;}
        public int getTotalHeight(){return rows*height+Math.max(0,rows-1)*spacing;}
    }
}
