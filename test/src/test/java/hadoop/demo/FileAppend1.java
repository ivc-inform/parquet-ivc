package hadoop.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.security.PrivilegedExceptionAction;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;

public class FileAppend1 {

  public static final String uri = "hdfs://localhost:9820/user/test/log.txt";

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
    conf.set("dfs.support.append", "true");

    // get a HDFS filesystem instance
    FileSystem fs = FileSystem.get(URI.create(uri), conf);

    //get if file append functionality is enabled
    boolean flag = Boolean.parseBoolean(fs.getConf().get("dfs.support.append"));
    System.out.println("dfs.support.append is set to be " + flag);

    if (flag) {
      UserGroupInformation ugi = UserGroupInformation.createRemoteUser("hdfs")
      ugi.doAs(
              new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                  FSDataOutputStream fsout;
                  if (!fs.exists(new Path(uri)))
                    fsout = fs.create(new Path(uri));
                  else
                    fsout = fs.append(new Path(uri));

                  // wrap the outputstream with a writer
                  PrintWriter writer = new PrintWriter(fsout);
                  writer.append(content);
                  writer.close();
                  return new Object();
                }
              }
      )

    } else {
      System.err.println("please set the dfs.support.append to be true");
    }

    fs.close();
  }

}

