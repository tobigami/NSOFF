package com.nsoz.skill;

import com.nsoz.option.SkillOption;
import org.json.simple.JSONObject;

public class Skill {

    public static final byte SKILL_AUTO_USE = 0;
    public static final byte SKILL_CLICK_USE_ATTACK = 1;
    public static final byte SKILL_CLICK_USE_BUFF = 2;
    public static final byte SKILL_CLICK_NPC = 3;
    public static final byte SKILL_CLICK_LIVE = 4;

    public int id;
    public byte point;
    public byte level;
    public short manaUse;
    public int coolDown;
    public short dx;
    public short dy;
    public byte maxFight;
    public SkillOption[] options;
    public long lastTimeUseThisSkill;
    public SkillTemplate template;

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("id", this.template.id);
        obj.put("point", this.point);
        return obj;
    }

    /**
     * Đánh dấu vừa dùng chiêu.
     *
     * Có sáu chỗ trong mã gán lastTimeUseThisSkill; gom về một hàm để lỡ cần đo lại nhịp đánh thì
     * chỉ phải thêm log ở đúng một chỗ, khỏi sót đường nào.
     */
    public void danhDau(String ai) {
        this.lastTimeUseThisSkill = System.currentTimeMillis();
    }


    public boolean isCooldown() {
        long currentTimeMillis = System.currentTimeMillis();
        long num = currentTimeMillis - lastTimeUseThisSkill;
        // Nới một chút cho lệch đồng hồ giữa hai máy.
        //
        // Client tự giữ nhịp bằng chính con số coolDown máy chủ gửi xuống, nhưng nó bấm giờ lúc
        // GỬI còn máy chủ bấm giờ lúc XỬ LÝ -- chênh nhau đúng phần đường truyền cộng xếp hàng.
        // Đo trên máy thật: client đếm đủ 100ms của nó thì máy chủ mới thấy 88--99ms, nên đòn bị
        // chặn dù client không hề bắn sớm. Tệ hơn, đòn bị chặn là mất luôn: client không bắn lại
        // ngay mà đợi hết một chu kỳ, biến nhịp 0,1s thành ~0,43s.
        //
        // Dung sai theo tỉ lệ chứ không cố định: 60ms là hạt bụi với chiêu chờ 45 giây nhưng lại
        // là quá nửa với chiêu chờ 0,1 giây. Lấy một phần tư thời gian chờ, tối đa 60ms -- đủ che
        // độ lệch đo được (12ms) với thừa biên, mà vẫn không thành cửa cho việc bắn nhanh gian
        // lận: client vẫn phải tự giữ nhịp, phần nới này nhỏ hơn hẳn một nhịp vẽ.
        long dungSai = Math.min(60L, this.coolDown / 4L);
        return num < (long) this.coolDown - dungSai;
    }
    public boolean isCloneSkill() { /// fix skill phân thân
        return this.template.id >= 67 && this.template.id <= 72;
    }
}
