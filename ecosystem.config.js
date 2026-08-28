// pm2 chạy được mọi tiến trình, không riêng gì Node -- ở đây là hai script sh.
//
//   nso        máy chủ game, cổng 14444
//   nso-share  thư mục tải file cho bạn bè trong tailnet, cổng 8080
//
// interpreter "none" để pm2 đừng cố chạy file bằng node. NSO_UNDER_PM2 để nút "Khởi động lại"
// trong game biết mà chỉ thoát, khỏi tự sinh tiến trình thứ hai giành cổng 14444.
module.exports = {
  apps: [{
    name: 'nso',
    script: './tools/run-server.sh',
    interpreter: 'none',
    cwd: __dirname,
    env: {
      NSO_UNDER_PM2: '1',
      // Hiện lên thanh tiêu đề cửa sổ Quản lý. Bản này là bản người ta đang chơi.
      NSO_MOI_TRUONG: 'prod',
    },
    autorestart: true,
    // Máy chủ nạp bản đồ, vật phẩm... mất vài chục giây; đừng coi đó là khởi động thất bại.
    min_uptime: '60s',
    max_restarts: 5,
    restart_delay: 5000,
    kill_timeout: 20000,
    merge_logs: true,
    time: true,
    out_file: 'build/server.log',
    error_file: 'build/server.log',
  }, {
    name: 'nso-share',
    script: './tools/run-share.sh',
    interpreter: 'none',
    cwd: __dirname,
    autorestart: true,
    // Bật cùng lúc với máy tính thì Tailscale có thể chưa lên; cứ thử lại vài lần rồi thôi.
    min_uptime: '10s',
    max_restarts: 10,
    restart_delay: 5000,
    merge_logs: true,
    time: true,
    out_file: 'build/share.log',
    error_file: 'build/share.log',
  }],
};
