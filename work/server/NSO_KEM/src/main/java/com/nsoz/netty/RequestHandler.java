package com.nsoz.netty;

import com.nsoz.convert.Converter;
import com.nsoz.item.Item;
import com.nsoz.lib.ParseData;
import com.nsoz.model.Char;
import com.nsoz.server.*;
import com.nsoz.store.ItemStore;
import com.nsoz.store.StoreManager;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import java.util.List;
import java.util.Map;

public class RequestHandler extends SimpleChannelInboundHandler<HttpObject> {
    private final IManagement managementService;

    public RequestHandler(IManagement managementService) {
        this.managementService = managementService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;

            if (req.method() == HttpMethod.POST && req.uri().equals("/server/remote/fdsgvfd11")) { // fix bug buff
                handlePostRequest(ctx, req);
            }
        }
    }

    private void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest content)  { // fix bug buff
        try{

            String clientIp = ((java.net.InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
            if (!clientIp.equals(Config.hostAdmin)) {
                System.out.println("phai dung ip config: " + clientIp);
                ctx.close();
                return;
            }

            ByteBuf jsonBuf = content.content();
            String json = jsonBuf.toString(CharsetUtil.UTF_8);

            // Giả sử dữ liệu POST ở dạng JSON
            JSONObject dataMap = (JSONObject) JSONValue.parse(json);
            JSONObject rs = new JSONObject();
            String password = (String) dataMap.get("password");
            if (!password.equals(Config.getInstance().getPassAdmin())) {
                rs.put("status", Status.ERR);
                rs.put("msg", "cút");
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(rs.toString(), CharsetUtil.UTF_8));

                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

                ctx.writeAndFlush(response);
                ctx.close();
                return;
            }
            String type = dataMap.get("type").toString();

            JSONObject data = (JSONObject) JSONValue.parse(dataMap.get("data").toString());
            ParseData parse = new ParseData((data));

            int result = processRequest(type, parse);


            rs.put("status", result);
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(rs.toString(), CharsetUtil.UTF_8));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

            ctx.writeAndFlush(response);
        } catch (Exception e) {

        }
    }

    private int processRequest(String type, ParseData parse) throws Exception {

        switch (type) {
            case "exchange":
                return addGold(parse);
            case "removeItemBag":
                return removeItemBag(parse);
            case "save_data":
                return saveData(parse);
            case "forceOut":
                return forceOut(parse);
            case "lock":
                int idUser = parse.getInt("idUser");
                Char _char = ServerManager.findCharById(idUser);
                if (_char == null) {
                    return Status.NO_USER;
                }
                _char.user.lock();
                return Status.SUCCESS;
            case "serverMaintenance":
                return serverMaintenance();
            case "setExp":
                int expX = parse.getInt("exp");
                Config.getInstance().setRateEXP(expX);
                return Status.SUCCESS;
            case "serverSaveData":
                return serverSaveData();
            case "server_alert":
                String text = parse.getString("message");
                GlobalService.getInstance().showAlert("Server", text);
                return Status.SUCCESS;
            case "add_item":

                return addItem(parse);
            case "removeItemBox":
            default:
                return Status.ERR;
        }
    }

    private int addItem(ParseData dataMap) {
        try {
            int idUser = dataMap.getInt("idUser");
            Char _char = ServerManager.findCharById(idUser);
            if (_char == null) {
                return Status.NO_USER;
            }
            JSONObject dataSend = (JSONObject) JSONValue.parse(dataMap.getString("dataSend").toString());
            ParseData parseSend = new ParseData((dataSend));
            int gold = parseSend.getInt("gold");
            int yen = parseSend.getInt("yen");
            int coin = parseSend.getInt("coin");
            JSONArray arrItem = (JSONArray) (new JSONParser().parse(parseSend.getString("items")));
            StringBuilder sb = new StringBuilder();
            sb.append("Chúc mừng, bạn đã được tặng").append("\n\n");
            if (gold > 0) {
                _char.addGold(gold);
                sb.append(String.format("- %s lượng", NinjaUtils.getCurrency(gold))).append("\n");
            }

            if (yen > 0) {
                _char.addYen(yen);
                sb.append(String.format("- %s yên", NinjaUtils.getCurrency(yen))).append("\n");
            }

            if (coin > 0) {
                _char.addCoin(coin);
                sb.append(String.format("- %s xu", NinjaUtils.getCurrency(coin))).append("\n");
            }
            int lent = arrItem.size();
            for (int i = 0; i < lent; i++) {
                JSONObject itemObj = (JSONObject) arrItem.get(i);
                Item newItem = new Item(itemObj);

                if (newItem.options == null || newItem.options.isEmpty()) {
                    ItemStore itemStore = null;
                    List<ItemStore> list = StoreManager.getInstance().getListEquipmentWithLevelRange(0, _char.level);
                    if (list == null || list.isEmpty()) {
                        newItem.initOption();
                    } else {
                        for (ItemStore itStore : list) {
                            if (itStore.getTemplate().id == newItem.template.id) {
                                itemStore = itStore;
                                break;
                            }
                        }
                    }

                    if (itemStore == null) {
                        if (newItem.template.isTypeBody() && newItem.template.level >= 90 && newItem.template.level <= 99) {

                            newItem.randomOptionItem9x(false);
                        } else if (newItem.template.isTypeBody() && newItem.template.level >= 100 && newItem.template.level <= 120) {

                            newItem.randomOptionItem10x(false);
                        } else {
                            newItem.initOption();
                        }

                        if (newItem.expire != -1) {
                            long expire = System.currentTimeMillis() + newItem.expire;
                            newItem.expire = expire;
                        }
                        newItem.nextUpdate2(newItem.upgrade);
                        _char.addItemToBag(newItem);
                        sb.append(String.format("- x%s %s", NinjaUtils.getCurrency(newItem.getQuantity()), newItem.template.name))
                                .append("\n");
                    } else {
                        Item itm = Converter.getInstance().toItem(itemStore, Converter.RANDOM_OPTION);
                        itm.next(newItem.upgrade);
                        itm.sys = _char.getSys();

                        if (newItem.expire == -1) {
                            long expire = System.currentTimeMillis() + newItem.expire;
                            itm.expire = expire;
                        }

                        _char.addItemToBag(itm);
                        sb.append(String.format("- x%s %s", NinjaUtils.getCurrency(newItem.getQuantity()), itm.template.name))
                                .append("\n");
                    }


                }else{

                    if (newItem.expire != -1) {
                        long expire = System.currentTimeMillis() + newItem.expire;
                        newItem.expire = expire;
                    }
                    newItem.nextUpdate2(newItem.upgrade);
                    _char.addItemToBag(newItem);
                    sb.append(String.format("- x%s %s", NinjaUtils.getCurrency(newItem.getQuantity()), newItem.template.name))
                            .append("\n");
                }
                // hạn code


            }
            _char.getService().showAlert("Admin tặng", sb.toString());

            return Status.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return Status.ERR;
        }

    }

    private int addGold(ParseData dataMap) {
        int idUser = dataMap.getInt("idUser");
        Char _char = ServerManager.findCharById(idUser);
        if (_char == null) {
            return Status.NO_USER;
        }
        String type = dataMap.getString("type");
        int gold = dataMap.getInt("gold");
        int yen = dataMap.getInt("yen");
        int coin = dataMap.getInt("coin");
        if (gold != 0) {
            if (type.equals("plus")) {
                _char.addGold(gold);
            } else {
                _char.addGold(-gold);
            }

        }
        if (yen != 0) {
            if (type.equals("plus")) {
                _char.addYen(yen);
            } else {
                _char.addYen(-yen);
            }

        }
        if (coin != 0) {
            if (type.equals("plus")) {
                _char.addCoin(coin);
            } else {
                _char.addCoin(-coin);
            }

        }
        _char.service.loadInfo();
        StringBuffer sb = new StringBuffer();
        sb.append("Bạn nhận được \n");
        sb.append("Xu: " + NinjaUtils.getCurrency(coin) + " xu\n");
        sb.append("Yên : " + NinjaUtils.getCurrency(yen) + " yên\n");
        sb.append("Luợng: " + NinjaUtils.getCurrency(gold) + " luợng\n");
        _char.service.showAlert("Admin", sb.toString());
        return Status.SUCCESS;
    }

    private int addCoin(Map<String, Object> dataMap) {
        int idUser = ((Double) dataMap.get("idUser")).intValue();
        Char _char = ServerManager.findCharById(idUser);
        if (_char == null) {
            return Status.NO_USER;
        }
        int coin = ((Double) dataMap.get("coin")).intValue();
        _char.addCoin(coin);
        _char.service.loadInfo();
        _char.service.serverMessage("Bạn nhận được " + NinjaUtils.getCurrency(coin) + " xu.");
        return Status.SUCCESS;
    }

    private int addYen(Map<String, Object> dataMap) {
        int idUser = ((Double) dataMap.get("idUser")).intValue();
        Char _char = ServerManager.findCharById(idUser);
        if (_char == null) {
            return Status.NO_USER;
        }
        int yen = ((Double) dataMap.get("yen")).intValue();
        _char.addYen(yen);
        _char.service.loadInfo();
        _char.service.serverMessage("Bạn nhận được " + NinjaUtils.getCurrency(yen) + " yên.");
        return Status.SUCCESS;
    }

    private int removeItemBag(ParseData dataMap) {
        int idUser = dataMap.getInt("idUser");
        Char _char = ServerManager.findCharById(idUser);
        if (_char == null) {
            return Status.NO_USER;
        }
        int itemId = dataMap.getInt("itemId");
        Item item = _char.getItemByIdInBag(itemId);
        if (item != null) {
            _char.removeItemBags(itemId, item.getQuantity());
            return Status.SUCCESS;
        }
        return Status.ERR;
    }

// Tương tự, tạo các phương thức khác như addItem, removeItemBox, serverAlert...

    private int saveData(ParseData dataMap) {
        int idUser = dataMap.getInt("idUser");
        Char _char = ServerManager.findCharById(idUser);
        if (_char == null) {
            return Status.NO_USER;
        }
        _char.saveData();
        return Status.SUCCESS;
    }

    private int forceOut(ParseData dataMap) {
        int idUser = dataMap.getInt("idUser");
        Char _char = ServerManager.findCharById(idUser);
        if (_char == null) {
            return Status.NO_USER;
        }
        _char.user.session.closeMessage();
        return Status.SUCCESS;
    }

    private int serverMaintenance() {
        (new Thread(new Runnable() {
            public void run() {
                try {
                    Server.maintance();
                    System.exit(0);
                } catch (Exception e) {
                    Log.logException("Lỗi commanf bảo trì: ", NinjaSchool.class, e);

                }

            }
        })).start();
        return Status.SUCCESS;
    }

    private int serverSaveData() {
        System.out.println("Lưu dữ liệu người chơi");
        Server.saveAllPlayer();
        Server.saveAll();
        System.out.println("Lưu dữ liệu người chơi xong");
        return Status.SUCCESS;
    }


}
