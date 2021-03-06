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
package org.apache.accumulo.core.metadata;

import java.util.SortedMap;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.clientImpl.ClientContext;
import org.apache.accumulo.core.clientImpl.Table;
import org.apache.accumulo.core.dataImpl.KeyExtent;

/**
 * A metadata servicer for the root table.<br>
 * The root table's metadata is serviced in zookeeper.
 */
class ServicerForRootTable extends MetadataServicer {

  private final ClientContext context;

  public ServicerForRootTable(ClientContext context) {
    this.context = context;
  }

  @Override
  public Table.ID getServicedTableId() {
    return RootTable.ID;
  }

  @Override
  public void getTabletLocations(SortedMap<KeyExtent,String> tablets)
      throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
    tablets.put(RootTable.EXTENT, context.getRootTabletLocation());
  }
}
