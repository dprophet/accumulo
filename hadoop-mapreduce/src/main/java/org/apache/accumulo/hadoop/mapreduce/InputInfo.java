/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.hadoop.mapreduce;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.accumulo.core.client.ClientInfo;
import org.apache.accumulo.core.client.ClientSideIteratorScanner;
import org.apache.accumulo.core.client.IsolatedScanner;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.hadoopImpl.mapreduce.InputInfoImpl;
import org.apache.hadoop.mapreduce.Job;

/**
 * Object containing all the information needed for the Map Reduce job. This object is passed to the
 * different Input Format types see {@link AccumuloInputFormat#setInfo(Job, InputInfo)}. It uses a
 * fluent API:
 *
 * <pre>
 * InputInfo.builder().clientInfo(info).table(name).scanAuths(auths).build();
 *
 * InputInfo.builder().clientProperties(props).table(name).scanAuths(auths).addIterator(cfg)
 *     .executionHints(hints).build();
 * </pre>
 *
 * @since 2.0
 */
public interface InputInfo {
  /**
   * @return the table name set using InputInfo.builder()...table(name)
   */
  String getTableName();

  /**
   * @return the client info set using InputInfo.builder().clientInfo(info)
   */
  ClientInfo getClientInfo();

  /**
   * @return the scan authorizations set using InputInfo.builder()...scanAuths(auths)
   */
  Authorizations getScanAuths();

  /**
   * @return the context set using InputInfo.builder()...classLoaderContext(context)
   */
  Optional<String> getContext();

  /**
   * @return the Ranges set using InputInfo.builder()...ranges(ranges)
   */
  Collection<Range> getRanges();

  /**
   * @return the ColumnFamily,ColumnQualifier Pairs set using
   *         InputInfo.builder()...fetchColumns(cfcqPairs)
   */
  Collection<IteratorSetting.Column> getFetchColumns();

  /**
   * @return the collection of IteratorSettings set using InputInfo.builder()...addIterator(cfg)
   */
  Collection<IteratorSetting> getIterators();

  /**
   * @return the SamplerConfiguration set using InputInfo.builder()...samplerConfiguration(cfg)
   */
  Optional<SamplerConfiguration> getSamplerConfig();

  /**
   * @return the Execution Hints set using InputInfo.builder()...executionHints(hints)
   */
  Map<String,String> getExecutionHints();

  /**
   * @return boolean if auto adjusting ranges or not
   */
  boolean isAutoAdjustRanges();

  /**
   * @return boolean if using scan isolation or not
   */
  boolean isScanIsolation();

  /**
   * @return boolean if using local iterators or not
   */
  boolean isLocalIterators();

  /**
   * @return boolean if using offline scan or not
   */
  boolean isOfflineScan();

  /**
   * @return boolean if using batch scanner or not
   */
  boolean isBatchScan();

  /**
   * Builder starting point for map reduce input format information.
   */
  static InputInfoBuilder.ClientParams builder() {
    return new InputInfoImpl.InputInfoBuilderImpl();
  }

  /**
   * Required build values to be set.
   *
   * @since 2.0
   */
  interface InputInfoBuilder {
    /**
     * Required params for builder
     *
     * @since 2.0
     */
    interface ClientParams {
      /**
       * Set the connection information needed to communicate with Accumulo in this job. ClientInfo
       * param can be created using {@link ClientInfo#from(Path)} or
       * {@link ClientInfo#from(Properties)}
       *
       * @param clientInfo
       *          Accumulo connection information
       */
      TableParams clientInfo(ClientInfo clientInfo);
    }

    /**
     * Required params for builder
     *
     * @since 2.0
     */
    interface TableParams {
      /**
       * Sets the name of the input table, over which this job will scan.
       *
       * @param tableName
       *          the table to use when the tablename is null in the write call
       */
      AuthsParams table(String tableName);
    }

    /**
     * Required params for builder
     *
     * @since 2.0
     */
    interface AuthsParams {
      /**
       * Sets the {@link Authorizations} used to scan. Must be a subset of the user's
       * authorizations. If none present use {@link Authorizations#EMPTY}
       *
       * @param auths
       *          the user's authorizations
       */
      InputFormatOptions scanAuths(Authorizations auths);
    }

    /**
     * Options for batch scan
     *
     * @since 2.0
     */
    interface BatchScanOptions {
      /**
       * @return newly created {@link InputInfo}
       */
      InputInfo build();
    }

    /**
     * Options for scan
     *
     * @since 2.0
     */
    interface ScanOptions extends BatchScanOptions {
      /**
       * @see InputFormatOptions#scanIsolation()
       */
      ScanOptions scanIsolation();

      /**
       * @see InputFormatOptions#localIterators()
       */
      ScanOptions localIterators();

      /**
       * @see InputFormatOptions#offlineScan()
       */
      ScanOptions offlineScan();
    }

    /**
     * Optional values to set using fluent API
     *
     * @since 2.0
     */
    interface InputFormatOptions {
      /**
       * Sets the name of the classloader context on this scanner
       *
       * @param context
       *          name of the classloader context
       */
      InputFormatOptions classLoaderContext(String context);

      /**
       * Sets the input ranges to scan for the single input table associated with this job.
       *
       * @param ranges
       *          the ranges that will be mapped over
       * @see TableOperations#splitRangeByTablets(String, Range, int)
       */
      InputFormatOptions ranges(Collection<Range> ranges);

      /**
       * Restricts the columns that will be mapped over for this job for the default input table.
       *
       * @param fetchColumns
       *          a collection of IteratorSetting.Column objects corresponding to column family and
       *          column qualifier. If the column qualifier is null, the entire column family is
       *          selected. An empty set is the default and is equivalent to scanning all columns.
       */
      InputFormatOptions fetchColumns(Collection<IteratorSetting.Column> fetchColumns);

      /**
       * Encode an iterator on the single input table for this job. It is safe to call this method
       * multiple times. If an iterator is added with the same name, it will be overridden.
       *
       * @param cfg
       *          the configuration of the iterator
       */
      InputFormatOptions addIterator(IteratorSetting cfg);

      /**
       * Set these execution hints on scanners created for input splits. See
       * {@link ScannerBase#setExecutionHints(java.util.Map)}
       */
      InputFormatOptions executionHints(Map<String,String> hints);

      /**
       * Causes input format to read sample data. If sample data was created using a different
       * configuration or a tables sampler configuration changes while reading data, then the input
       * format will throw an error.
       *
       * @param samplerConfig
       *          The sampler configuration that sample must have been created with inorder for
       *          reading sample data to succeed.
       *
       * @see ScannerBase#setSamplerConfiguration(SamplerConfiguration)
       */
      InputFormatOptions samplerConfiguration(SamplerConfiguration samplerConfig);

      /**
       * Disables the automatic adjustment of ranges for this job. This feature merges overlapping
       * ranges, then splits them to align with tablet boundaries. Disabling this feature will cause
       * exactly one Map task to be created for each specified range. Disabling has no effect for
       * batch scans at it will always automatically adjust ranges.
       * <p>
       * By default, this feature is <b>enabled</b>.
       *
       * @see #ranges(Collection)
       */
      InputFormatOptions disableAutoAdjustRanges();

      /**
       * Enables the use of the {@link IsolatedScanner} in this job.
       * <p>
       * By default, this feature is <b>disabled</b>.
       */
      ScanOptions scanIsolation();

      /**
       * Enables the use of the {@link ClientSideIteratorScanner} in this job. This feature will
       * cause the iterator stack to be constructed within the Map task, rather than within the
       * Accumulo TServer. To use this feature, all classes needed for those iterators must be
       * available on the classpath for the task.
       * <p>
       * By default, this feature is <b>disabled</b>.
       */
      ScanOptions localIterators();

      /**
       * Enable reading offline tables. By default, this feature is disabled and only online tables
       * are scanned. This will make the map reduce job directly read the table's files. If the
       * table is not offline, then the job will fail. If the table comes online during the map
       * reduce job, it is likely that the job will fail.
       * <p>
       * To use this option, the map reduce user will need access to read the Accumulo directory in
       * HDFS.
       * <p>
       * Reading the offline table will create the scan time iterator stack in the map process. So
       * any iterators that are configured for the table will need to be on the mapper's classpath.
       * <p>
       * One way to use this feature is to clone a table, take the clone offline, and use the clone
       * as the input table for a map reduce job. If you plan to map reduce over the data many
       * times, it may be better to the compact the table, clone it, take it offline, and use the
       * clone for all map reduce jobs. The reason to do this is that compaction will reduce each
       * tablet in the table to one file, and it is faster to read from one file.
       * <p>
       * There are two possible advantages to reading a tables file directly out of HDFS. First, you
       * may see better read performance. Second, it will support speculative execution better. When
       * reading an online table speculative execution can put more load on an already slow tablet
       * server.
       * <p>
       * By default, this feature is <b>disabled</b>.
       */
      ScanOptions offlineScan();

      /**
       * Enables the use of the {@link org.apache.accumulo.core.client.BatchScanner} in this job.
       * Using this feature will group Ranges by their source tablet, producing an InputSplit per
       * tablet rather than per Range. This batching helps to reduce overhead when querying a large
       * number of small ranges. (ex: when doing quad-tree decomposition for spatial queries)
       * <p>
       * In order to achieve good locality of InputSplits this option always clips the input Ranges
       * to tablet boundaries. This may result in one input Range contributing to several
       * InputSplits.
       * <p>
       * Note: calls to {@link #disableAutoAdjustRanges()} is ignored when BatchScan is enabled.
       * <p>
       * This configuration is incompatible with:
       * <ul>
       * <li>{@link #offlineScan()}</li>
       * <li>{@link #localIterators()}</li>
       * <li>{@link #scanIsolation()}</li>
       * </ul>
       * <p>
       * By default, this feature is <b>disabled</b>.
       */
      BatchScanOptions batchScan();

      /**
       * @return newly created {@link InputInfo}
       */
      InputInfo build();
    }
  }
}
