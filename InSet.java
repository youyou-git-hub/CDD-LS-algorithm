import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class InSet implements Iterable<Integer>{
    final int MAX_INT;
    final private int[] hash_table;
    final private int[] list;
    private int size;

    InSet(int max){
        MAX_INT = max;
        hash_table = new int[max+1];
        list = new int[max+1];
        size = 0;

        Arrays.fill(hash_table, -1);
    }

    InSet(InSet o){
        MAX_INT = o.MAX_INT;
        hash_table = o.hash_table.clone();
        list = o.list.clone();
        size = o.size;
    }

    InSet(int max, List<Integer> from_list){
        MAX_INT = max;
        hash_table = new int[max+1];
        list = new int[max+1];
        size = 0;

        Arrays.fill(hash_table, -1);
        for(int d : from_list){
            add(d);
        }
    }


    void clear(){
        Arrays.fill(hash_table, -1);
        size = 0;
    }

    void copy_from(InSet o){
        System.arraycopy(o.hash_table, 0, hash_table, 0, hash_table.length);
        System.arraycopy(o.list, 0, list,0, o.size);
        size = o.size;
    }

    int[] to_array(){
        return Arrays.copyOfRange(list, 0, size);
    }

    int get_from_list(int index){
        return list[index];
    }

    int size(){
        return size;
    }

    void add(int item){
        if(hash_table[item] >= 0)return;
        list[size] = item;
        hash_table[item] = size;
        ++size;
    }

    void remove(int item){
        int loc = hash_table[item];
        if(loc == -1)return;
        hash_table[item]  = -1;
        int last = list[size - 1];
        if(last != item) {
            list[loc] = last;
            hash_table[last] = loc;
        }
        --size;
    }

    boolean contains(int item){
        return hash_table[item] >= 0;
    }

    static InSet union(InSet a, InSet b){
        InSet new_set = new InSet(Math.max(a.MAX_INT, b.MAX_INT));

        for(int d : a){
            new_set.add(d);
        }
        for(int d : b){
            new_set.add(d);
        }
        return new_set;
    }

    static InSet intersection(InSet a, InSet b){
        InSet new_set = new InSet(Math.max(a.MAX_INT, b.MAX_INT));

        for(int d : a){
            if(b.contains(d)){
                new_set.add(d);
            }
        }
        return new_set;
    }

    boolean equals(InSet s){
        if(size != s.size()){
            return false;
        }

        for(int d : s){
            if(!contains(d)){
                return false;
            }
        }
        return true;
    }

    boolean isEmpty(){
        return size == 0;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            private int curr_index = 0;
            @Override
            public boolean hasNext() {
                return curr_index < size;
            }

            @Override
            public Integer next() {
                return list[curr_index++];
            }
        };
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("{");
        for(int d : this){
            str.append(d).append(", ");
        }
        if(size>0) {
            str.delete(str.length() - 2, str.length());
        }
        str.append("}");
        return str.toString();
    }

    public void check_set(){
        int count = 0;
        for(int i=0; i<hash_table.length; ++i){
            int loc = hash_table[i];
            if(loc > -1){
                ++count;
                if(list[loc] != i){
                    throw new Error("Set error 1!");
                }
            }
        }
        if(count != size){
            throw new Error("Set error 2!");
        }
    }
}

