import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class CDD_LS2 {
    public static void main(String[] args) throws IOException {
        CDD_LS2 tt = new CDD_LS2();
        tt.star();
    }

    int vertex_num;         //结点数
    int[][] Graph;          //结点矩阵
    int[] Tabu;             //禁忌数组
    int Tabu_length;        //禁忌长度
    int Tabu_Iter;          //禁忌迭代次数
    int Iteration;          //运行次数
    int disturbIteration;   //扰动迭代次数
    int[] V_degree;         //节点度数矩阵
    int[] low;              //low[u]：表示顶点u及其子树中的点，通过反向边，能够回溯到的最早的点（dfn最小）的dfn值。
    int[] dfn;              //深度优先搜索，dfn[n] = x表示节点n在第x次被搜索到
    int deep;               //搜索深度
    int f;                 //评估函数
    int min_f;              //历史出现最小的冲突数
    int max_f;              //历史出现的最大冲突数
    int[] move;             //存储动作数组
    int a;                  //评估函数系数,表示重要性
    int root;               //根节点
    InSet D_zpj;     //支配集
    InSet D_bzpj;    //被支配集
    InSet D_wgj;     //未被支配集
    InSet D_2cds;    //双联通支配集
    InSet D_articulation;    //割点集

    InSet Sbest;
    Random r;
    String filename;        //文件名
    long startTime;         //获取开始时间
    long endTime;         //获取开始时间

    //读文件创建矩阵
    void Creat_Graph(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String s;
        String[] strings;
        int a, b;
        s = br.readLine();
        strings = s.split(" ");
        vertex_num = Integer.parseInt(strings[0]);
        Graph = new int[vertex_num + 1][vertex_num + 1];
        while ((s = br.readLine()) != null) {
            strings = s.split(" ");
            a = Integer.parseInt(strings[0]);
            b = Integer.parseInt(strings[1]);
            Graph[a][b] = 1;
            Graph[b][a] = 1;
        }
        br.close();
    }

    //初始化
    void initialization() throws IOException {
        filename = "/Users/qinhuigang/Desktop/双连通支配集/所有算例/LMS/v30_d10.dat";
        Creat_Graph(filename);
        dfn = new int[vertex_num + 1];
        low = new int[vertex_num + 1];
        Tabu = new int[vertex_num + 1];
        Tabu_Iter = 0;
        Iteration = 0;
        move = new int[2];
        a = 70;
        min_f = 10000;
        max_f = 0;
        V_degree = new int[vertex_num + 1];
        Sbest = new InSet(vertex_num + 1);
        D_zpj = new InSet(vertex_num + 1);
        D_bzpj = new InSet(vertex_num + 1);
        D_wgj = new InSet(vertex_num + 1);
        D_2cds = new InSet(vertex_num + 1);
        D_articulation = new InSet(vertex_num + 1);
        r = new Random();
        deep = 0;
        init_D_zpj();
        getV_Degree();
    }

    //初始化支配集
    void init_D_zpj() {
        for (int i = 1; i < vertex_num + 1; i++) {
            D_zpj.add(i);
        }
    }

    //根据支配集初始化其他集合，并且初始化支配连通度
    void init_CDD(InSet Sbest) {
        Arrays.fill(V_degree, 0);
        for (Integer i : Sbest) {
            for (int j = 1; j < vertex_num + 1; j++) {
                if (Graph[i][j] != 0) {
                    if (Check_e_belong_D_set(j, Sbest)) {
                        V_degree[i]++;
                    } else {
                        V_degree[j]--;
                    }
                }
            }
        }
        D_zpj.clear();
        D_bzpj.clear();
        D_wgj.clear();
        D_zpj.copy_from(Sbest);

        for (int j = 1; j < vertex_num + 1; j++) {
            if (V_degree[j] < 0) {
                D_bzpj.add(j);
            } else if (V_degree[j] == 0) {
                D_wgj.add(j);
            }
        }
        D_wgj.remove(0);

    }

    //实验之前检查数据是否联通
    boolean Check_Data() {
        boolean b = true;
        for (int i = 1; i < vertex_num + 1; i++) {
            if (V_degree[i] == 0) {
                b = false;
                break;
            }
        }
        return b;
    }

    //获取每个节点度数
    void getV_Degree() {
        for (int i = 1; i < Graph.length; ++i) {
            for (int j = 1; j < Graph.length; ++j) {
                if (Graph[i][j] != 0) {
                    V_degree[i]++;
                }
            }
            //System.out.print(V_degree[i]+ " ");
        }
    }

    //选取节点最小的节点删除
    int find_MinDegree_Vertex() {
        int index = -1;
        int MinDegree = vertex_num + 1;
        int same = 1;
        for (Integer i : D_zpj) {
            if (V_degree[i] < MinDegree && Tabu[i] <= Tabu_Iter) {
                index = i;
                MinDegree = V_degree[i];
                same = 0;
            } else if (V_degree[i] == MinDegree && Tabu[i] <= Tabu_Iter) {
                same++;
                if (r.nextInt(same) == 1) {
                    index = i;
                }
            }
        }
        return index;
    }

    //删除节点后，修改支配集节点度数,支配集中节点为正数，表示其相邻的点为支配集的个数
    //被支配集中的节点为负数，负号为标志 ， 数值表示其与支配集中节点连接的个数；
    //未被支配的节点为0；
    //删除动作
    void Delete(int e) {
        D_zpj.remove(e);
        V_degree[e] = 0;
        for (int i = 1; i < Graph.length; i++) {
            if (Graph[i][e] == 1) {
                if (Check_e_belong_D_set(i, D_zpj)) {
                    V_degree[i]--;
                    V_degree[e]--;
                } else {
                    V_degree[i]++;
                    if (V_degree[i] == 0) {
                        D_bzpj.remove(i);
                        D_wgj.add(i);
                    }
                }
            }
        }
        //最后依据符号判定e节点以及其领域属于什么集合
        if (V_degree[e] == 0) {
            D_wgj.add(e);
        } else if (V_degree[e] < 0) {
            D_bzpj.add(e);
        }
    }

    //增加动作
    void Add(int e) {
        D_zpj.add(e);
        D_bzpj.remove(e);
        V_degree[e] = 0;
        for (int i = 1; i < Graph.length; i++) {
            if (Graph[i][e] == 1) {
                if (Check_e_belong_D_set(i, D_zpj)) {
                    V_degree[i]++;
                    V_degree[e]++;
                } else {
                    V_degree[i]--;
                    if (Check_e_belong_D_set(i, D_wgj)) {
                        D_wgj.remove(i);
                        D_bzpj.add(i);
                    }
                }
            }
        }
    }

    void find_move2() {
        InSet D_zpjplus = new InSet(D_zpj);
        InSet D_bzpjplus = new InSet(D_bzpj);   //应该为bzpj+wgj
        int index_f;
        int Min_f = 100000;
        int Tabu_Min_f = 100000;
        int Tabu_f;
        int Tabu_x = -1, Tabu_y = -1;
        int same = 1;
        int Tabu_same = 1;
        if (min_f > f) {
            min_f = f;
            disturbIteration = 0;
        }
        for (Integer i : D_articulation) {
            D_zpjplus.remove(i);
        }
        for (Integer i : D_zpjplus) {
            Delete(i);
            tar_jan(init_tarjan(D_zpj), -1, D_zpj);
            index_f = a * D_wgj.size() + (100 - a) * D_articulation.size();
            if (index_f == 0) {
                move[0] = i;
                move[1] = -1;
                //System.out.println("直接减少不添加");
                D_2cds.clear();
                D_2cds.copy_from(D_zpj);
                break;
            }
            for (Integer j : D_bzpjplus) {
                if (V_degree[j] == 0) {
                    ////删除节点i之后节点J的度数为零，则表示节点J仅与支配集中的节点I相连
                } else {
                    Add(j);
                    if (a == 100) {
                        index_f = a * D_wgj.size() + (100 - a) * D_articulation.size();
                        Tabu_f = a * D_wgj.size() + (100 - a) * D_articulation.size();
                        if (Tabu[i] > Tabu_Iter || Tabu[j] > Tabu_Iter) {
                            if (Tabu_f < Tabu_Min_f) {
                                Tabu_x = i;
                                Tabu_y = j;
                                Tabu_same = 1;
                                Tabu_Min_f = Tabu_f;
                            } else if (Tabu_f == Tabu_Min_f) {
                                Tabu_same++;
                                if (r.nextInt(Tabu_same) == 1) {
                                    Tabu_x = i;
                                    Tabu_y = j;
                                }
                            }
                        } else {
                            if (index_f < Min_f) {
                                move[0] = i;
                                move[1] = j;
                                same = 1;
                                Min_f = index_f;
                            } else if (index_f == Min_f) {
                                same++;
                                if (r.nextInt(same) == 1) {
                                    move[0] = i;
                                    move[1] = j;
                                }
                            }
                        }
                    } else {
                        tar_jan(init_tarjan(D_zpj), -1, D_zpj);
                        index_f = a * D_wgj.size() + (100 - a) * D_articulation.size();
                        Tabu_f = a * D_wgj.size() + (100 - a) * D_articulation.size();
                        if (Tabu[i] > Tabu_Iter || Tabu[j] > Tabu_Iter) {
                            if (Tabu_f < Tabu_Min_f) {
                                Tabu_x = i;
                                Tabu_y = j;
                                Tabu_same = 1;
                                Tabu_Min_f = Tabu_f;
                            } else if (Tabu_f == Tabu_Min_f) {
                                Tabu_same++;
                                if (r.nextInt(Tabu_same) == 1) {
                                    Tabu_x = i;
                                    Tabu_y = j;
                                }
                            }
                        } else {
                            if (index_f < Min_f) {
                                move[0] = i;
                                move[1] = j;
                                same = 1;
                                Min_f = index_f;
                            } else if (index_f == Min_f) {
                                same++;
                                if (r.nextInt(same) == 1) {
                                    move[0] = i;
                                    move[1] = j;
                                }
                            }
                        }
                    }
                    Delete(j);
                }
            }
            Add(i);
        }
        if (Tabu_Min_f < min_f) {
            move[0] = Tabu_x;
            move[1] = Tabu_y;
        }
    }

    //执行交换动作
    void make_move() {
        find_move2();
        if (move[1] == -1) {
            Delete(move[0]);
            Tabu_length = D_zpj.size() / 5;
            //Tabu_length = 10;
            Tabu[move[0]] = Tabu_Iter + Tabu_length;
            Tabu_Iter++;
        } else {
            Delete(move[0]);
            Tabu_length = D_bzpj.size() / 5;
            //Tabu_length = 10;
            Tabu[move[0]] = Tabu_Iter + Tabu_length;
            Add(move[1]);
            //Tabu_length = (D_zpj.size() - D_articulation.size()) / 3;
            //Tabu[move[1]] = Tabu_Iter + Tabu_length;
            Tabu_Iter++;
        }
    }

    //检验节点属于什么集合,true 为属于
    boolean Check_e_belong_D_set(int e, InSet D) {
        boolean b = false;
        for (Integer i : D) {
            if (e == i) {
                b = true;
                break;
            }
        }
        return b;
    }


    //使用之前初始化DFN,LOW,D_articulation,deep,并从D_zpj中随机选择一个节点当做root
    int init_tarjan(InSet D_zpj) {
        for (int i = 1; i < vertex_num + 1; i++) {
            dfn[i] = 0;
            low[i] = 0;
        }
        deep = 0;
        D_articulation.clear();
        int[] D_zpj_array = D_zpj.to_array();
        root = D_zpj_array[0];
        return root;
    }

    //Tarjan算法
    void tar_jan(int v, int father, InSet D_zpj) {
        dfn[v] = low[v] = ++deep;
        int child = 0;
        for (Integer i : D_zpj) {
            if (Graph[v][i] == 1 && v != i) {
                if (dfn[i] == 0) {           //节点v未被访问，则(u,v)为树边
                    child++;
                    tar_jan(i, v, D_zpj);
                    low[v] = Math.min(low[v], low[i]);
                    if (v != root && low[i] >= dfn[v]) {     //不为根结点但是满足第二类条件的节点
                        D_articulation.add(v);

                    }
                    if (v == root && child >= 2) {   //  如果当前节点是根节点并且儿子个数大于等于2，则满足第一类节点，为割点
                        D_articulation.add(v);

                    }
                } else if (i != father) {   // //节点v已访问，则(u,v)为回边
                    low[v] = Math.min(low[v], dfn[i]);
                }
            }
        }
    }

    void setDisturbIteration() {
        if (max_f < f) {
            max_f = f;
            disturbIteration = 0;
        }
        if (min_f > f) {
            min_f = f;
            Sbest.copy_from(D_zpj);
            disturbIteration = 0;
        }
        if (f > min_f && f < max_f) {
            disturbIteration++;
        }
    }

    //扰动
    void disturb() {
        disturbIteration = 0;
        int[] disturbAraay = new int[vertex_num + 1];
        int length = D_zpj.size() / 3;
        init_CDD(Sbest);
        for (int i = 0; i < length; i++) {
            InSet D_zpjplus = new InSet(D_zpj);
            InSet D_bzpjplus = new InSet(D_bzpj);
            tar_jan(init_tarjan(D_zpj), -1, D_zpj);
            for (Integer j : D_articulation) {
                D_zpjplus.remove(j);
            }
            for (int k = 1; k < disturbAraay.length; k++) {
                if (disturbAraay[k] == 1) {
                    D_zpjplus.remove(k);
                }
            }
            if (D_zpjplus.size() != 0) {
                int index = 0;
                int x = 0;
                int randomNumber = r.nextInt(D_zpjplus.size() + 1);
                for (Integer k : D_zpjplus) {
                    index++;
                    if (index == randomNumber) {
                        x = k;
                    }
                }
                Delete(x);
                disturbAraay[x] = 1;
                for (Integer k : D_bzpjplus) {
                    if (disturbAraay[k] == 1 || V_degree[k] == 0) {
                        D_bzpjplus.remove(k);
                    }
                }
                if (D_bzpjplus.isEmpty()) {
                    Add(x);
                    disturbAraay[x] = 0;
                } else {
                    for (Integer k : D_bzpjplus) {
                        if(Check_e_belong_D_set(k,D_bzpj)){
                            disturbAraay[k] = 1;
                            Add(k);
                            break;
                        }
                    }

                }
            }
        }
        D_wgj.remove(0);
    }

    //-----------------------------开始实验----------------------------------
    void star() throws IOException {
        initialization();
        startTime = System.currentTimeMillis(); //获取开始时间
        if (!Check_Data()) {
            System.out.println("数据集未联通，停止实验！");
        } else {
            System.out.println("-----------------------开始实验---------------------------------");
            int k = D_zpj.size();
            while (Iteration < 10000 && D_zpj.size() > 2) {
                if (D_zpj.size() > (k - 1)) {
                    int v = find_MinDegree_Vertex();
                    Delete(v);
                    tar_jan(init_tarjan(D_zpj), -1, D_zpj);
                    f = a * D_wgj.size() + (100 - a) * D_articulation.size();
                } else {
                    if (f > 0) {
                        if (disturbIteration > 2000) {
                            System.out.println("扰动前支配集大小为：" + D_zpj.size() + "; " + D_zpj);
                            System.out.println("扰动前被支配集大小为：" + D_bzpj.size() + "; " + D_bzpj);
                            System.out.println("扰动前无关集大小为：" + D_wgj.size() + "; " + D_wgj);
                            disturb();
                            System.out.println("开始扰动");
                            System.out.println("扰动后支配集大小为：" + D_zpj.size() + "; " + D_zpj);
                            System.out.println("扰动后被支配集大小为：" + D_bzpj.size() + "; " + D_bzpj);
                            System.out.println("扰动后无关集大小为：" + D_wgj.size() + "; " + D_wgj);
                            System.out.println();
                            tar_jan(init_tarjan(D_zpj), -1, D_zpj);
                            f = a * D_wgj.size() + (100 - a) * D_articulation.size();
                            max_f = f;
                        } else {
                            make_move();
                            tar_jan(init_tarjan(D_zpj), -1, D_zpj);
                            f = a * D_wgj.size() + (100 - a) * D_articulation.size();
                            Iteration++;
                            setDisturbIteration();
                        }
                    } else {
                        k = D_zpj.size();
                        Iteration = 0;
                        min_f = 100000;
                        max_f = 0;
                        Sbest.clear();
                        D_2cds.clear();
                        D_2cds.copy_from(D_zpj);
                        System.out.println("D_2cds number is " + D_2cds.size());
                        System.out.println("D_2cds number is " + "：" + check_D_2cds_plus(D_2cds) + ";" + D_2cds);
                        endTime = System.currentTimeMillis(); //获取结束时间
                        System.out.println("共用时：" + (endTime - startTime) + "ms"); //输出运行时间
                    }
                }
            }
        }
    }

    //用来记录局部最优格局
    //结束后再次检验双联通支配集是否符合
    boolean check_D_2cds_plus(InSet D_2cds) {
        HashSet<Integer> a_plus = new HashSet<>();
        int[] integers = D_2cds.to_array();
        for (int i = 1; i < vertex_num + 1; i++) {
            a_plus.add(i);
        }
        for (Integer integer : integers) {
            a_plus.remove(integer);
            for (int j = 1; j < Graph.length; j++) {
                if (Graph[integer][j] != 0) {
                    a_plus.remove(j);
                }
            }
        }
        boolean b = a_plus.isEmpty();
        int index = 0;
        for (Integer i : D_2cds) {
            //System.out.print(i + "->");
            for (Integer j : D_2cds) {
                if (Graph[i][j] == 1) {
                    index++;
                }
            }
            if (index < 2) {
                b = false;
            }
            index = 0;
        }
        return b;
    }

}
