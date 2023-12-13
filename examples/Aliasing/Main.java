public class Main {

    static int[] getIntArray(){
        System.out.println("Getting an array of int");
        return new int[]{ 12, 25, 31, 47, 82, 99, 107 };
    }

    public static void main(String[] args) {

        getIntArray()[0] = 87;
        var a = (getIntArray()[1] = 995);
        getIntArray()[0] += 573;
        var b = (getIntArray()[3] *= 35);
        System.out.println(a*a + b*b + a - b);

        var x = Foo.instance().x = 1007;
        Foo.instance().x = 202;
        System.out.println(x);
        int y = Foo.instance().x -= 578;
        int z = Foo.instance().x <<= 3;
        Foo.instance().x -= -1;
        System.out.println(y + " " + z);

        var r = getIntArray()[2];
        var s = Foo.instance().x;
        System.out.println(r + s);
        System.out.println(Foo.instance().descr());
    }

}
