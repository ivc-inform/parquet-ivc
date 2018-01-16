package hadoop.demo;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

public class FileAppend1 {

  public static final String uri = "hdfs://localhost:9820/user/test/log.txt";
  
  public static void main(String[] args) throws IOException, InterruptedException {
   
    Configuration conf = new Configuration();
    conf.setBoolean("dfs.support.append", true);
    conf.set("fs.defaultFS", "hdfs://localhost:9820");
    //conf.setInt("dfs.replication", 3);
    conf.setBoolean("dfs.permissions", false);
    System.setProperty("HADOOP_USER_NAME", "hdfs");

    // get a HDFS filesystem instance
    FileSystem fs = FileSystem.get(conf);

    //get if file append functionality is enabled
    boolean flag = Boolean.parseBoolean(fs.getConf().get("dfs.support.append"));
    System.out.println("dfs.support.append is set to be " + flag);

    if (flag) {
      Path p = new Path(uri);
      fs.setReplication(p,(short) 1);
      FSDataOutputStream fsout;

      if (!fs.exists(p)) {
        fsout = fs.create(p);
        fsout.writeBytes("creating and writing into file.\n");
      }
      else {
        FileStatus status = fs.getFileStatus(p);
        fsout = fs.append(p);
        fsout.writeBytes("appending into file.\n");
      }
      
      fsout.hsync();

     /* FSDataInputStream inputStream = fs.open(new Path(uri));
      String out = IOUtils.toString(inputStream, "UTF-8");
      System.out.println(out);
      inputStream.close();*/

      if (fs != null)
        fs.close();
      if (fsout != null)
        fsout.close();
    }
  }

}

