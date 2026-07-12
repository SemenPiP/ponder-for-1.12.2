package net.createmod.catnip.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

public class UniqueLinkedList<E> extends LinkedList<E> {
    private static final long serialVersionUID = 1L;
    private final Set<E> contained = new HashSet<E>();

    public boolean contains(Object value) { return contained.contains(value); }
    public boolean add(E value) { return contained.add(value) && super.add(value); }
    public void add(int index, E value) { if (contained.add(value)) super.add(index, value); }
    public void addFirst(E value) { if (contained.add(value)) super.addFirst(value); }
    public void addLast(E value) { if (contained.add(value)) super.addLast(value); }
    public boolean offer(E value) { return add(value); }
    public boolean offerFirst(E value) { if (!contained.add(value)) return false; super.addFirst(value); return true; }
    public boolean offerLast(E value) { if (!contained.add(value)) return false; super.addLast(value); return true; }
    public void push(E value) { addFirst(value); }
    public boolean addAll(Collection<? extends E> values) { return addAll(size(), values); }
    public boolean addAll(int index, Collection<? extends E> values) {
        boolean changed = false;
        int insertion = index;
        for (E value : values) if (contained.add(value)) { super.add(insertion++, value); changed = true; }
        return changed;
    }
    public E set(int index, E value) {
        E previous = super.get(index);
        if (java.util.Objects.equals(previous, value)) return previous;
        if (contained.contains(value)) throw new IllegalArgumentException("Duplicate element: " + value);
        E result = super.set(index, value); contained.remove(previous); contained.add(value); return result;
    }
    public boolean remove(Object value) {
        boolean removed = super.remove(value); if (removed) contained.remove(value); return removed;
    }
    public E remove(int index) { E value = super.remove(index); contained.remove(value); return value; }
    public E removeFirst() { E value = super.removeFirst(); contained.remove(value); return value; }
    public E removeLast() { E value = super.removeLast(); contained.remove(value); return value; }
    public E poll() { return pollFirst(); }
    public E pollFirst() { if (isEmpty()) return null; return removeFirst(); }
    public E pollLast() { if (isEmpty()) return null; return removeLast(); }
    public E pop() { return removeFirst(); }
    public void clear() { super.clear(); contained.clear(); }
    public Iterator<E> iterator() { return listIterator(0); }
    public Iterator<E> descendingIterator() {
        final ListIterator<E> iterator = listIterator(size());
        return new Iterator<E>() {
            public boolean hasNext() { return iterator.hasPrevious(); }
            public E next() { return iterator.previous(); }
            public void remove() { iterator.remove(); }
        };
    }
    public ListIterator<E> listIterator(final int index) {
        final ListIterator<E> delegate = super.listIterator(index);
        return new ListIterator<E>() {
            private E last;
            private boolean canModify;
            public boolean hasNext() { return delegate.hasNext(); }
            public E next() { last = delegate.next(); canModify = true; return last; }
            public boolean hasPrevious() { return delegate.hasPrevious(); }
            public E previous() { last = delegate.previous(); canModify = true; return last; }
            public int nextIndex() { return delegate.nextIndex(); }
            public int previousIndex() { return delegate.previousIndex(); }
            public void remove() { delegate.remove(); contained.remove(last); canModify = false; }
            public void set(E value) {
                if (!canModify) throw new IllegalStateException();
                if (!java.util.Objects.equals(last, value) && contained.contains(value))
                    throw new IllegalArgumentException("Duplicate element: " + value);
                delegate.set(value); contained.remove(last); contained.add(value); last = value;
            }
            public void add(E value) { if (contained.add(value)) delegate.add(value); canModify = false; }
        };
    }
    public Object clone() { UniqueLinkedList<E> copy = new UniqueLinkedList<E>(); copy.addAll(this); return copy; }
}
