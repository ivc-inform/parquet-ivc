package hadoop.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class FileAppend1 {

 public static final String uri = "hdfs://localhost:9820/user/zhouyaofei/log.txt";

 /**
  * @param args
  * @throws IOException
  */
 public static void main(String[] args) throws IOException {
  // TODO Auto-generated method stub

  if (args.length == 0) {
   System.err.println("wrong argument list");
  }

  // get the content user want to append
  String content = args[0];

  // instantiate a configuration class
  Configuration conf = new Configuration();

  // get a HDFS filesystem instance
  FileSystem fs = FileSystem.get(URI.create(uri), conf);

  //get if file append functionality is enabled
  boolean flag = Boolean.getBoolean(fs.getConf()
    .get("dfs.support.append"));
  System.out.println("dfs.support.append is set to be " + flag);

  if (flag) {
   FSDataOutputStream fsout = fs.append(new Path(uri));

   // wrap the outputstream with a writer
   PrintWriter writer = new PrintWriter(fsout);
   writer.append(content);
   writer.close();
  } else {
   System.err.println("please set the dfs.support.append to be true");
  }

  fs.close();
 }

}

