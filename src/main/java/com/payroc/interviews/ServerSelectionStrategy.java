package com.payroc.interviews;

import java.util.List;

/**
 * Strategy for choosing a backend server from a list.
 */
public interface ServerSelectionStrategy {
    Server select(List<Server> servers);
}
