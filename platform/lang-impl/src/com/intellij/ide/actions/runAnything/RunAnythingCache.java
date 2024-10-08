// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.actions.runAnything;

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.groups.RunAnythingCompletionGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingGroup;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import com.intellij.util.xmlb.annotations.XMap;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
@State(name = "RunAnythingCache", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class RunAnythingCache implements PersistentStateComponent<RunAnythingCache.State> {
  private final State mySettings = new State();

  public static RunAnythingCache getInstance(Project project) {
    return project.getService(RunAnythingCache.class);
  }

  /**
   * @return true is group is visible; false if it's hidden
   */
  public boolean isGroupVisible(@NotNull RunAnythingGroup group) {
    Boolean visible = mySettings.myKeys.get(group.getTitle());
    if (visible != null) {
      return visible;
    }
    if (group instanceof RunAnythingCompletionGroup) {
      String name = ((RunAnythingCompletionGroup<?, ?>)group).getProvider().getClass().getCanonicalName();
      Boolean providerValue = mySettings.myKeys.get(name);
      if (providerValue != null) {
        return providerValue;
      }
    }
    return true;
  }

  /**
   * Saves group visibility flag
   *
   * @param key     to store visibility flag
   * @param visible true if group should be shown
   */
  public void saveGroupVisibilityKey(@NotNull String key, boolean visible) {
    mySettings.myKeys.put(key, visible);
  }

  @Override
  public @NotNull State getState() {
    return mySettings;
  }

  @Override
  public void loadState(@NotNull State state) {
    XmlSerializerUtil.copyBean(state, mySettings);

    updateNewProvidersGroupVisibility(mySettings);
  }

  /**
   * Updates group visibilities store for new providers
   */
  private static void updateNewProvidersGroupVisibility(@NotNull State settings) {
    StreamEx.of(RunAnythingProvider.EP_NAME.getExtensions())
      .filter(provider -> provider.getCompletionGroupTitle() != null)
      .distinct(RunAnythingProvider::getCompletionGroupTitle)
      .filter(provider -> !settings.myKeys.containsKey(provider.getCompletionGroupTitle()))
      .forEach(provider -> settings.myKeys.put(provider.getCompletionGroupTitle(), true));
  }

  public static final class State {
    @XMap(entryTagName = "visibility", keyAttributeName = "group", valueAttributeName = "flag") private final @NotNull Map<String, Boolean> myKeys =
      StreamEx.of(RunAnythingProvider.EP_NAME.getExtensions())
              .filter(provider -> provider.getCompletionGroupTitle() != null)
              .distinct(RunAnythingProvider::getCompletionGroupTitle)
              .collect(Collectors.toMap(RunAnythingProvider::getCompletionGroupTitle, group -> true));

    @XCollection(elementName = "command") private final @NotNull List<String> myCommands = new ArrayList<>();

    public @NotNull List<String> getCommands() {
      return myCommands;
    }
  }
}