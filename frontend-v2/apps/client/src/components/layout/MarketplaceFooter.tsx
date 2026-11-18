import { FiArrowUpRight, FiShield } from 'react-icons/fi';

export function MarketplaceFooter() {
  return <footer className="market-footer"><div className="market-container market-footer__grid"><div><a className="market-logo" href="#top"><span>M</span><div><b>MAKON</b><small>marketplace</small></div></a><p>Технологичный маркетплейс для покупателей и локального бизнеса Узбекистана.</p></div><div><b>Покупателям</b><a href="#catalog">Каталог</a><a href="#categories">Категории</a><a href="#trust">Как это работает</a></div><div><b>Бизнесу</b><a href="http://localhost:4200" target="_blank" rel="noreferrer">Кабинет продавца <FiArrowUpRight /></a></div><div><b>О сервисе</b><span><FiShield />Продавцы проходят проверку</span><span>Заказы и остатки обновляются в реальном времени</span></div></div><div className="market-container market-footer__bottom"><span>© 2026 Makon Marketplace</span><span>Сделано для удобных покупок</span></div></footer>;
}
