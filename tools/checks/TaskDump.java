public final class TaskDump {
    public static void main(String[] a) {
        nsoff.WorldData wd = nsoff.WorldData.get();
        for (int t = 0; t <= 3; t++) {
            nsoff.WorldData.TaskDef d = wd.task(t);
            if (d == null) { System.out.println("task " + t + " missing"); continue; }
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < d.npcs.length; i++) sb.append(d.npcs[i]).append(' ');
            System.out.println("task " + d.id + " '" + d.name + "' steps=" + d.subs.length
                    + " npcs=[" + sb.toString().trim() + "]");
        }
    }
}
