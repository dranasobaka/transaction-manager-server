package io.transatron.transaction.manager.functional.steps;

import lombok.Value;

@Value(staticConstructor = "of")
public class TestUser {
    long userId;
    long sessionId;
}