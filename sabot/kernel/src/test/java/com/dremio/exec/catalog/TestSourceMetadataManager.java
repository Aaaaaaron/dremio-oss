/*
 * Copyright (C) 2017-2018 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.inject.Provider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.dremio.datastore.KVStore;
import com.dremio.exec.proto.UserBitShared;
import com.dremio.exec.store.DatasetRetrievalOptions;
import com.dremio.exec.store.StoragePlugin;
import com.dremio.service.namespace.NamespaceKey;
import com.dremio.service.namespace.NamespaceService;
import com.dremio.service.namespace.dataset.proto.DatasetConfig;
import com.dremio.service.namespace.dataset.proto.ReadDefinition;
import com.dremio.service.namespace.source.proto.MetadataPolicy;
import com.dremio.service.scheduler.SchedulerService;
import com.dremio.test.UserExceptionMatcher;

public class TestSourceMetadataManager {
  private Provider<Integer> provider = () -> 800;

  @Rule
  public final ExpectedException thrownException = ExpectedException.none();

  @Test
  public void deleteUnavailableDataset() throws Exception {
    NamespaceService ns = mock(NamespaceService.class);
    when(ns.getDataset(any()))
        .thenReturn(
            new DatasetConfig()
                .setTag("0")
                .setReadDefinition(new ReadDefinition())
        );

    boolean[] deleted = new boolean[] {false};
    doAnswer(invocation -> {
      deleted[0] = true;
      return null;
    }).when(ns).deleteDataset(any(), anyString());

    StoragePlugin.CheckResult result = mock(StoragePlugin.CheckResult.class);
    when(result.getStatus())
        .thenReturn(StoragePlugin.UpdateStatus.DELETED);

    StoragePlugin sp = mock(StoragePlugin.class);
    when(sp.checkReadSignature(any(), any(), any()))
        .thenReturn(result);

    ManagedStoragePlugin msp = mock(ManagedStoragePlugin.class);
    when(msp.getName())
        .thenReturn(new NamespaceKey("joker"));
    when(msp.unwrap(any()))
        .thenReturn(sp);
    when(msp.getMetadataPolicy())
        .thenReturn(new MetadataPolicy().setDeleteUnavailableDatasets(false));
    when(msp.getMaxMetadataColumns())
      .thenReturn(provider);

    //noinspection unchecked
    SourceMetadataManager manager = new SourceMetadataManager(
        mock(SchedulerService.class),
        true,
        ns,
        mock(KVStore.class),
        msp
    );

    assertEquals(StoragePlugin.UpdateStatus.DELETED,
        manager.refreshDataset(new NamespaceKey(""), DatasetRetrievalOptions.DEFAULT));
    assertTrue(deleted[0]);
  }

  @Test
  public void doNotDeleteUnavailableDataset() throws Exception {
    NamespaceService ns = mock(NamespaceService.class);
    when(ns.getDataset(any()))
        .thenReturn(new DatasetConfig().setReadDefinition(new ReadDefinition()));

    doThrow(new IllegalStateException("should not invoke deleteDataset()"))
        .when(ns)
        .deleteDataset(any(), anyString());

    StoragePlugin.CheckResult result = mock(StoragePlugin.CheckResult.class);
    when(result.getStatus())
        .thenReturn(StoragePlugin.UpdateStatus.DELETED);

    StoragePlugin sp = mock(StoragePlugin.class);
    when(sp.checkReadSignature(any(), any(), any()))
        .thenReturn(result);

    ManagedStoragePlugin msp = mock(ManagedStoragePlugin.class);
    when(msp.getName())
        .thenReturn(new NamespaceKey("joker"));
    when(msp.unwrap(any()))
        .thenReturn(sp);
    when(msp.getMetadataPolicy())
        .thenReturn(new MetadataPolicy().setDeleteUnavailableDatasets(false));
    when(msp.getMaxMetadataColumns())
      .thenReturn(provider);

    //noinspection unchecked
    SourceMetadataManager manager = new SourceMetadataManager(
        mock(SchedulerService.class),
        true,
        ns,
        mock(KVStore.class),
        msp
    );

    assertEquals(StoragePlugin.UpdateStatus.UNCHANGED,
        manager.refreshDataset(new NamespaceKey(""),
            DatasetRetrievalOptions.DEFAULT.toBuilder()
                .setDeleteUnavailableDatasets(false)
                .build()));
  }

  @Test
  public void deleteUnavailableDatasetWithoutDefinition() throws Exception {
    NamespaceService ns = mock(NamespaceService.class);
    when(ns.getDataset(any()))
        .thenReturn(
            new DatasetConfig()
                .setTag("0")
        );

    boolean[] deleted = new boolean[] {false};
    doAnswer(invocation -> {
      deleted[0] = true;
      return null;
    }).when(ns).deleteDataset(any(), anyString());

    StoragePlugin.CheckResult result = mock(StoragePlugin.CheckResult.class);
    when(result.getStatus())
        .thenReturn(StoragePlugin.UpdateStatus.DELETED);

    StoragePlugin sp = mock(StoragePlugin.class);
    when(sp.checkReadSignature(any(), any(), any()))
        .thenReturn(result);

    ManagedStoragePlugin msp = mock(ManagedStoragePlugin.class);
    when(msp.getName())
        .thenReturn(new NamespaceKey("joker"));
    when(msp.unwrap(any()))
        .thenReturn(sp);
    when(msp.getMetadataPolicy())
        .thenReturn(new MetadataPolicy().setDeleteUnavailableDatasets(false));
    when(msp.getMaxMetadataColumns())
      .thenReturn(provider);

    //noinspection unchecked
    SourceMetadataManager manager = new SourceMetadataManager(
        mock(SchedulerService.class),
        true,
        ns,
        mock(KVStore.class),
        msp
    );

    assertEquals(StoragePlugin.UpdateStatus.DELETED,
        manager.refreshDataset(new NamespaceKey(""), DatasetRetrievalOptions.DEFAULT));
    assertTrue(deleted[0]);
  }

  @Test
  public void doNotDeleteUnavailableDatasetWithoutDefinition() throws Exception {
    NamespaceService ns = mock(NamespaceService.class);
    when(ns.getDataset(any())).thenReturn(new DatasetConfig());

    doThrow(new IllegalStateException("should not invoke deleteDataset()"))
        .when(ns)
        .deleteDataset(any(), anyString());

    StoragePlugin.CheckResult result = mock(StoragePlugin.CheckResult.class);
    when(result.getStatus())
        .thenReturn(StoragePlugin.UpdateStatus.DELETED);

    StoragePlugin sp = mock(StoragePlugin.class);
    when(sp.checkReadSignature(any(), any(), any()))
        .thenReturn(result);

    ManagedStoragePlugin msp = mock(ManagedStoragePlugin.class);
    when(msp.getName())
        .thenReturn(new NamespaceKey("joker"));
    when(msp.unwrap(any()))
        .thenReturn(sp);
    when(msp.getMetadataPolicy())
        .thenReturn(new MetadataPolicy().setDeleteUnavailableDatasets(false));
    when(msp.getMaxMetadataColumns())
      .thenReturn(provider);

    //noinspection unchecked
    SourceMetadataManager manager = new SourceMetadataManager(
        mock(SchedulerService.class),
        true,
        ns,
        mock(KVStore.class),
        msp
    );

    assertEquals(StoragePlugin.UpdateStatus.UNCHANGED,
        manager.refreshDataset(new NamespaceKey(""),
            DatasetRetrievalOptions.DEFAULT.toBuilder()
                .setDeleteUnavailableDatasets(false)
                .build()));
  }

  @Test
  public void checkForceUpdate() throws Exception {
    NamespaceService ns = mock(NamespaceService.class);
    when(ns.getDataset(any())).thenReturn(null);

    StoragePlugin.CheckResult result = mock(StoragePlugin.CheckResult.class);
    when(result.getStatus())
        .thenReturn(StoragePlugin.UpdateStatus.DELETED);

    StoragePlugin sp = mock(StoragePlugin.class);
    when(sp.checkReadSignature(any(), any(), any()))
        .thenReturn(result);

    final boolean[] forced = new boolean[]{false};
    doAnswer(invocation -> {
      forced[0] = DatasetRetrievalOptions.class.cast(invocation.getArguments()[2]).forceUpdate();
      return null;
    }).when(sp).getDataset(any(), any(), any());

    ManagedStoragePlugin msp = mock(ManagedStoragePlugin.class);
    when(msp.getName())
        .thenReturn(new NamespaceKey("joker"));
    when(msp.unwrap(any()))
        .thenReturn(sp);
    when(msp.getMetadataPolicy())
        .thenReturn(new MetadataPolicy().setDeleteUnavailableDatasets(false));
    when(msp.getMaxMetadataColumns())
        .thenReturn(provider);

    //noinspection unchecked
    SourceMetadataManager manager = new SourceMetadataManager(
        mock(SchedulerService.class),
        true,
        ns,
        mock(KVStore.class),
        msp
    );

    try {
      manager.refreshDataset(new NamespaceKey(""),
          DatasetRetrievalOptions.DEFAULT.toBuilder()
              .setForceUpdate(true)
              .build());
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Unable to find requested table"));
    }

    assertTrue(forced[0]);
  }

  @Test
  public void exceedMaxColumnLimit() throws Exception {
    NamespaceService ns = mock(NamespaceService.class);
    when(ns.getDataset(any()))
        .thenReturn(null);

    StoragePlugin sp = mock(StoragePlugin.class);

    doThrow(new ColumnCountTooLargeException("too large"))
        .when(sp)
        .getDataset(any(), any(), any());

    ManagedStoragePlugin msp = mock(ManagedStoragePlugin.class);
    when(msp.getName())
        .thenReturn(new NamespaceKey("joker"));
    when(msp.unwrap(any()))
        .thenReturn(sp);
    when(msp.getMetadataPolicy())
        .thenReturn(new MetadataPolicy());

    //noinspection unchecked
    SourceMetadataManager manager = new SourceMetadataManager(
        mock(SchedulerService.class),
        true,
        ns,
        mock(KVStore.class),
        msp
    );

    thrownException.expect(new UserExceptionMatcher(UserBitShared.DremioPBError.ErrorType.VALIDATION,
        "Using datasets with more than 1 columns is currently disabled"));
    manager.refreshDataset(new NamespaceKey(""),
        DatasetRetrievalOptions.DEFAULT.toBuilder()
            .setForceUpdate(true)
            .setMaxMetadataLeafColumns(1)
            .build());
  }
}
