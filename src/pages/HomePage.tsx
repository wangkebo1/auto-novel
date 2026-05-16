const HomePage = () => {
  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <section className="grid gap-8 md:grid-cols-12 md:items-end">
        <div className="md:col-span-7 md:pb-10">
          <p className="text-sm uppercase tracking-[0.3em] opacity-60">静态站点 Demo</p>
          <h1 className="mt-4 max-w-xl font-title text-5xl leading-tight md:text-7xl">
            山麓边的
            <br />
            艺术与咖啡空间
          </h1>
          <p className="mt-6 max-w-lg text-lg leading-relaxed opacity-90">
            这是一个纯静态页面示例，包含品牌介绍、活动信息与到访入口，可直接部署到 CloudBase 静态托管。
          </p>
        </div>

        <div className="md:col-span-5 md:-mb-10">
          <div className="rotate-1 border border-[#25221d]/20 bg-[#d8c3a5] p-6 shadow-[10px_10px_0_0_#25221d]">
            <p className="text-xs uppercase tracking-[0.2em]">本周展讯</p>
            <h2 className="mt-3 text-2xl font-semibold">木刻与日光</h2>
            <p className="mt-3 text-sm leading-relaxed">3 月 16 日 - 3 月 31 日，开放工作坊 + 手冲体验。</p>
          </div>
        </div>
      </section>

      <section className="mt-16 grid gap-5 md:grid-cols-3">
        <article className="border border-[#25221d]/20 bg-[#efe7d7] p-6">
          <h3 className="text-lg font-semibold">空间导览</h3>
          <p className="mt-2 text-sm opacity-85">双层展区、阅读角与庭院吧台，适合半日停留。</p>
        </article>
        <article className="border border-[#25221d]/20 bg-[#f8f4ea] p-6 md:-mt-6">
          <h3 className="text-lg font-semibold">手作课程</h3>
          <p className="mt-2 text-sm opacity-85">每周四开放木作体验，限 12 人，线上预约制。</p>
        </article>
        <article className="border border-[#25221d]/20 bg-[#efe7d7] p-6">
          <h3 className="text-lg font-semibold">咖啡菜单</h3>
          <p className="mt-2 text-sm opacity-85">单一产区豆轮换供应，搭配季节甜点。</p>
        </article>
      </section>
    </div>
  );
};

export default HomePage;
