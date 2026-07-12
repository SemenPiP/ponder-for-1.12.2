package net.createmod.catnip.layout;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class PaginationState {
    private final boolean usesPagination;
    private int pageIndex;
    private final int elementsPerPage, elementCount;
    public PaginationState(){this(false,0,1,1);}
    public PaginationState(boolean usesPagination,int elementsPerPage,int elementCount){this(usesPagination,0,elementsPerPage,elementCount);}
    public PaginationState(boolean usesPagination,int pageIndex,int elementsPerPage,int elementCount){
        if(elementsPerPage<=0)throw new IllegalArgumentException("elementsPerPage must be positive");
        if(elementCount<0)throw new IllegalArgumentException("elementCount cannot be negative");
        this.usesPagination=usesPagination;this.elementsPerPage=elementsPerPage;this.elementCount=elementCount;
        this.pageIndex=Math.max(0,Math.min(pageIndex,Math.max(0,getMaxPages()-1)));
    }
    public boolean usesPagination(){return usesPagination;} public int getPageIndex(){return pageIndex;}
    public int getMaxPages(){return !usesPagination?1:Math.max(1,(elementCount+elementsPerPage-1)/elementsPerPage);}
    public int getElementsPerPage(){return elementsPerPage;} public int getElementCount(){return elementCount;}
    public int getStartIndex(){return usesPagination?pageIndex*elementsPerPage:0;}
    public int getCurrentPageElementCount(){return !usesPagination?elementCount:Math.max(0,Math.min(elementsPerPage,elementCount-getStartIndex()));}
    public void iterateForCurrentPage(BiConsumer<Integer,Integer> consumer){for(int i=0;i<getCurrentPageElementCount();i++)consumer.accept(i,i+getStartIndex());}
    public boolean hasPreviousPage(){return usesPagination&&pageIndex>0;} public boolean hasNextPage(){return usesPagination&&pageIndex+1<getMaxPages();}
    public void nextPage(){if(hasNextPage())pageIndex++;} public void previousPage(){if(hasPreviousPage())pageIndex--;}
    public boolean equals(Object o){if(this==o)return true;if(!(o instanceof PaginationState))return false;PaginationState p=(PaginationState)o;return usesPagination==p.usesPagination&&pageIndex==p.pageIndex&&elementsPerPage==p.elementsPerPage&&elementCount==p.elementCount;}
    public int hashCode(){return Objects.hash(usesPagination,pageIndex,elementsPerPage,elementCount);}
    public String toString(){return "PaginationState[usesPagination="+usesPagination+", pageIndex="+pageIndex+", elementsPerPage="+elementsPerPage+", elementCount="+elementCount+"]";}
}
