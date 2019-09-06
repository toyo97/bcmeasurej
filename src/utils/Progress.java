package utils;

public class Progress {

    private int imgCount;
    private int imgTotal;
    private int cellCount;
    private int cellTotal;

    private String img;

    public Progress(int imgTotal) {
        this.imgTotal = imgTotal;
    }

    public void show() {
        System.out.print("'" + img + "' " + imgCount + "/" + imgTotal + " - cell n. " + cellCount + " of " + cellTotal + "\r");
    }

    public void stepImg() {
        this.imgCount++;
    }

    public void stepCell() {
        this.cellCount++;
    }

    public void resetCellCount(int newCellTotal, String img) {
        this.cellTotal = newCellTotal;
        this.cellCount = 0;
        this.img = img;
    }

//    SETTER
    public void setImgCount(int imgCount) {
        this.imgCount = imgCount;
    }

    public void setImgTotal(int imgTotal) {
        this.imgTotal = imgTotal;
    }

    public void setCellCount(int cellCount) {
        this.cellCount = cellCount;
    }

    public void setCellTotal(int cellTotal) {
        this.cellTotal = cellTotal;
    }

//    GETTER
    public int getImgCount() {
        return imgCount;
    }

    public int getImgTotal() {
        return imgTotal;
    }

    public int getCellCount() {
        return cellCount;
    }

    public int getCellTotal() {
        return cellTotal;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }
}
