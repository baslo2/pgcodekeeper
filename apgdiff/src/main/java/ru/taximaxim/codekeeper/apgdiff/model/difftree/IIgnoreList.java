package ru.taximaxim.codekeeper.apgdiff.model.difftree;

import java.util.List;

public interface IIgnoreList {

    List<IgnoredObject> getList();
    void add(IgnoredObject rule);
    void setShow(boolean isShow);
    boolean isShow();
}
