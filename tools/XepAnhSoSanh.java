import java.awt.*; import java.awt.image.*; import java.io.*; import javax.imageio.ImageIO;
public class W {
  // Xếp các bản theo hàng, canh giữa theo tâm mắt để so bề rộng vùng đỏ cho công bằng.
  public static void main(String[] a) throws Exception {
    int k=(a.length-2)/3, scale=1, bg=Integer.decode(a[1]);
    BufferedImage[] im=new BufferedImage[k]; String[] lb=new String[k];
    int w=0,h=0;
    for(int i=0;i<k;i++){
      BufferedImage f=ImageIO.read(new File(a[2+i*3]));
      int n=Integer.parseInt(a[3+i*3]), fh=f.getHeight()/n;
      im[i]=f.getSubimage(0,(n-1)*fh,f.getWidth(),fh);
      lb[i]=a[4+i*3];
      w=Math.max(w,im[i].getWidth()*scale); h+=im[i].getHeight()*scale+26;
    }
    BufferedImage out=new BufferedImage(w+24,h+10,BufferedImage.TYPE_INT_RGB);
    Graphics2D g=out.createGraphics();
    g.setColor(new Color(bg)); g.fillRect(0,0,out.getWidth(),out.getHeight());
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setFont(new Font("Helvetica",Font.BOLD,12));
    int y=8;
    for(int i=0;i<k;i++){
      g.setColor(Color.WHITE); g.drawString(lb[i],10,y+11); y+=16;
      g.drawImage(im[i],(out.getWidth()-im[i].getWidth()*scale)/2,y,im[i].getWidth()*scale,im[i].getHeight()*scale,null);
      y+=im[i].getHeight()*scale+10;
    }
    g.dispose(); ImageIO.write(out,"png",new File(a[0])); System.out.println("ok");
  }
}
