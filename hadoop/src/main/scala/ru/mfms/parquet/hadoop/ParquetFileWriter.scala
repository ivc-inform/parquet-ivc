package ru.mfms.parquet.hadoop

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.hadoop.{ParquetFileWriter ⇒ HParquetFileWriter}
import org.apache.parquet.schema.MessageType

class ParquetFileWriter(configuration: Configuration, schema: MessageType, file: Path, mode: HParquetFileWriter.Mode, rowGroupSize: Long, maxPaddingSize: Int) extends HParquetFileWriter(configuration, schema, file, mode, rowGroupSize, maxPaddingSize) {
    
}
