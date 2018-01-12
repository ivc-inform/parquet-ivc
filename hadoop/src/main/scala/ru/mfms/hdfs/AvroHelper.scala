package ru.mfms.hdfs

import com.sksamuel.avro4s.{AvroSchema, SchemaFor}
import com.typesafe.scalalogging.LazyLogging
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import ru.mfms.parquet.avro.{AvroParquetReader, AvroParquetWriter}
import org.apache.parquet.hadoop.ParquetFileWriter.Mode.OVERWRITE
import org.apache.parquet.hadoop.metadata.CompressionCodecName.UNCOMPRESSED
import org.apache.parquet.hadoop.{ParquetFileWriter, ParquetReader, ParquetWriter}

object AvroHelper extends LazyLogging {
    def getAvroParquetWriter[T <: AnyRef](path: Path, conf: Configuration = new Configuration())(implicit schemaFor: SchemaFor[T]): ParquetWriter[GenericRecord] = {
        val schema: Schema = AvroSchema[T]
        //logger debug (s"Schema: [${schema.toString(true)}")

        AvroParquetWriter.builder[GenericRecord](path)
          .withSchema(schema)
          .withConf(conf)
          .withCompressionCodec(UNCOMPRESSED)
          .withWriteMode(OVERWRITE)
          .build()
    }

    def getAvroParquetReader(path: Path, conf: Configuration = new Configuration()): ParquetReader[GenericRecord] =
        AvroParquetReader.builder[GenericRecord](path)
          .withDataModel(new GenericData())
          .withConf(conf)
          .build()
}
