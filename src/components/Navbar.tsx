import { Link } from "react-router-dom";

const Navbar = () => {
  return (
    <header className="sticky top-0 z-20 border-b border-[#25221d]/15 bg-[#f4f0e7]/90 backdrop-blur">
      <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link to="/" className="font-title text-2xl tracking-wide">
          Cedar Landing
        </Link>
        <a href="#visit" className="rounded-full border border-[#25221d] px-4 py-1.5 text-sm transition hover:bg-[#25221d] hover:text-[#f4f0e7]">
          预约参观
        </a>
      </nav>
    </header>
  );
};

export default Navbar;
