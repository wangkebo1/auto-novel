const Footer = () => {
  return (
    <footer className="border-t border-[#25221d]/15 bg-[#efe7d7]">
      <div className="mx-auto grid max-w-6xl grid-cols-1 gap-6 px-6 py-10 md:grid-cols-3">
        <div>
          <h3 className="font-title text-xl">Cedar Landing</h3>
          <p className="mt-2 text-sm opacity-80">一个用于演示 CloudBase 静态托管的示例站点。</p>
        </div>
        <div>
          <p className="text-sm uppercase tracking-[0.2em] opacity-60">开放时间</p>
          <p className="mt-2">周二至周日 09:00 - 18:30</p>
        </div>
        <div id="visit" className="md:text-right">
          <p className="text-sm uppercase tracking-[0.2em] opacity-60">联系</p>
          <p className="mt-2">hello@cedar-landing.demo</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
