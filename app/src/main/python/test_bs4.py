from bs4 import BeautifulSoup
def test():
    soup=BeautifulSoup('<div><b>BeautifulSoup works</b></div>','html.parser')
    return soup.b.get_text()
