package org.getalp.dbnary.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class Struct {
  private final Map<String, Stack<Object>> delegate = new HashMap<>();

  private Stack<Object> getStack(String key) {
    return delegate.getOrDefault(key, new Stack<>());
  }

  // get/set behaviour
  public void set(String key, Object value) {
    Stack<Object> stack = getStack(key);
    stack.clear();
    stack.push(value);
  }

  public Optional<Object> get(String key) {
    return this.peek(key);
  }

  // stack behaviour
  public void push(String key, Object value) {
    Stack<Object> stack = getStack(key);
    stack.push(value);
  }

  public Optional<Object> pop(String key) {
    Stack<Object> stack = getStack(key);
    if (stack.empty())
      return Optional.empty();
    else
      return Optional.ofNullable(stack.pop());
  }

  public Optional<Object> peek(String key) {
    Stack<Object> stack = getStack(key);
    if (stack.empty())
      return Optional.empty();
    else
      return Optional.ofNullable(stack.peek());
  }

  public void delete(String key) {
    Stack<Object> stack = delegate.get(key);
    if (stack != null)
      delegate.remove(key);
  }

  // Set behaviour
  public void add(String key, Object value) {
    Stack<Object> stack = getStack(key);
    stack.push(value);
  }

  public boolean remove(String key, Object value) {
    Stack<Object> stack = delegate.get(key);
    return stack.remove(value);
  }

  public boolean contains(String key, Object value) {
    Stack<Object> stack = delegate.get(key);
    return stack.contains(value);
  }

}
